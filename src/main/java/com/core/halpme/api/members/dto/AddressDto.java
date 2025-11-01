package com.core.halpme.api.members.dto;

import com.core.halpme.api.members.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String zipCode;
    private String basicAddress;
    private String detailAddress;
    private String direction;

    public static AddressDto toDto(Address address) {

        return AddressDto.builder()
                .zipCode(address.getZipCode())
                .basicAddress(address.getBasicAddress())
                .detailAddress(address.getDetailAddress())
                .direction(address.getDirection())
                .build();
    }
}

