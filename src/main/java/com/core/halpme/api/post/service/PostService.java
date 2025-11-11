package com.core.halpme.api.post.service;

import com.core.halpme.api.members.entity.Address;
import com.core.halpme.api.members.entity.Member;
import com.core.halpme.api.members.repository.MemberRepository;
import com.core.halpme.api.post.dto.*;
import com.core.halpme.api.post.entity.Post;
import com.core.halpme.api.post.entity.PostStatus;
import com.core.halpme.api.post.repository.PostRepository;

import com.core.halpme.api.rank.service.RankService;
import com.core.halpme.common.exception.ConflictException;
import com.core.halpme.common.exception.NotFoundException;
import com.core.halpme.common.exception.UnauthorizedException;
import com.core.halpme.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;


@Service
@RequiredArgsConstructor
public class PostService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final RankService rankService;

    // 봉사 신청글 생성
    @Transactional
    @CacheEvict(value = "postsCache", key = "'allPosts'")
    public void createPost(String email, PostCreateRequestDto request) throws NotFoundException, UnauthorizedException {

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_USER.getMessage()));

        Address address = request.getAddress();

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .requestDate(request.getRequestDate())
                .startHour(request.getStartHour())
                .endHour(request.getEndHour())
                .address(address)
                .member(member)
                .postStatus(PostStatus.WAITING)
                .build();

        postRepository.save(post);
    }

    // 내 봉사 신청글 전체 조회
    @Transactional(readOnly = true)
    public List<MyPostListResponseDto> getMyPostList(String email) {

        List<Post> myPosts = postRepository.findPostByMemberEmail(email);

        return myPosts.stream()
                .map(MyPostListResponseDto::toDto)
                .toList();
    }

    // 내 봉사 참여글(내역) 전체 조회
    @Transactional(readOnly = true)
    public List<MyVolunteerPostListResponseDto> getMyVolunteerPosts(String email) {

        List<PostStatus> statuses = List.of(PostStatus.COMPLETED, PostStatus.AUTHENTICATED);

        List<Post> myPosts = postRepository.findByVolunteerEmailAndPostStatusInOrderByRequestDateDesc(email, statuses);

        return myPosts.stream()
                .map(MyVolunteerPostListResponseDto::toDto)
                .toList();
    }

    // 봉사 참여
    @Transactional
    public void participateAsVolunteer(Long postId, String volunteerEmail) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_RESOURCE.getMessage()));

        if (post.getVolunteer() != null) {
            throw new ConflictException(ErrorStatus.BAD_REQUEST_ALREADY_ASSIGNED_VOLUNTEER.getMessage());
        }

        Member volunteer = memberRepository.findByEmail(volunteerEmail)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_VOLUNTEER.getMessage()));

        post.assignVolunteer(volunteer);
    }

    //봉사자 인증
    @Transactional
    public void authenticatePost(Long postId, String email) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_RESOURCE.getMessage()));

        if(!post.getMember().getEmail().equals(email)) {
            throw new UnauthorizedException(ErrorStatus.BAD_REQUEST_POST_WRITER_NOT_SAME_USER.getMessage());
        }

        //봉사자 존재 여부 확인 후 Rank 업데이트
        if(post.getVolunteer() != null) {
            int volunteerHours = post.calculateVolunteerHours();
            rankService.updateRank(post.getVolunteer(), post, volunteerHours);
        }
        else {
            throw new NotFoundException(ErrorStatus.NOT_FOUND_VOLUNTEER.getMessage());
        }

        // 봉사 요청글의 상태를 "완료"로 변경
        post.updateActivityStatus(PostStatus.COMPLETED);
    }

    // 전체 봉사 신청글 조회
    //@Cacheable(value = "postsCache", key = "'allPosts'")
    public List<PostTotalListResponseDto> getTotalPostList() {

        List<Post> posts = postRepository.findAll();

        List<Post> sortedPosts = posts.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .toList();

        return sortedPosts.stream()
                .map(PostTotalListResponseDto::toDto)
                .toList();
    }

    // 봉사 신청글 상세 조회
    public PostDetailResponseDto getPostDetail(Long postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_RESOURCE.getMessage()));

        return PostDetailResponseDto.toDto(post);
    }

    // 봉사 신청글 수정
    @Transactional
    @CacheEvict(value = "postsCache", key = "'allPosts'")
    public void updatePost(Long postId, String email, PostCreateRequestDto request) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_RESOURCE.getMessage()));

        if(!post.getMember().getEmail().equals(email)) {
            throw new UnauthorizedException(ErrorStatus.BAD_REQUEST_POST_WRITER_NOT_SAME_USER.getMessage());
        }

        Address address = request.getAddress();

        post.updateTitle(request.getTitle());
        post.updateContent(request.getContent());
        post.updateAddress(address);
    }

    // 봉사 신청글 삭제
    @Transactional
    @CacheEvict(value = "postsCache", key = "'allPosts'")
    public void deletePost(Long postId, String email) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_RESOURCE.getMessage()));

        if(!post.getMember().getEmail().equals(email)) {
            throw new UnauthorizedException(ErrorStatus.BAD_REQUEST_POST_WRITER_NOT_SAME_USER.getMessage());
        }

        postRepository.delete(post);
    }

    @Transactional
    @CacheEvict(value = "postsCache", key = "'allPosts'")
    public void createDummyPosts() {
        // 더미 작성자 찾기 또는 생성 (없으면 예외 대신 생성하거나 처리)
        Member member = memberRepository.findByEmail("dummy@halpme.com")
                .orElseThrow(() -> new RuntimeException("더미 회원이 없습니다."));

        IntStream.rangeClosed(1, 500).forEach(i -> {
            Post post = Post.builder()
                    .title("더미 게시글 제목 " + i)
                    .content("더미 게시글 내용 " + i)
                    .requestDate(LocalDate.now())
                    .startHour(LocalTime.of(9, 0))
                    .endHour(LocalTime.of(12, 0))
                    .postStatus(PostStatus.WAITING)
                    .address(new Address("12345", "서울시", "상세주소" + i, "찾아오시는길" + i))
                    .member(member)
                    .build();

            postRepository.save(post);
        });
    }
}
