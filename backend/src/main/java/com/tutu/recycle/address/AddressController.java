package com.tutu.recycle.address;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tutu.recycle.common.ApiResponse;
import com.tutu.recycle.common.BusinessException;
import com.tutu.recycle.security.CurrentUser;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Validated
@RestController
@PreAuthorize("hasRole('RESIDENT')")
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressMapper addressMapper;
    public AddressController(AddressMapper addressMapper) { this.addressMapper = addressMapper; }

    @GetMapping
    public ApiResponse<List<Address>> list() {
        return ApiResponse.success(addressMapper.selectList(new QueryWrapper<Address>()
                .eq("user_id", CurrentUser.id()).eq("is_deleted", 0).orderByDesc("is_default").orderByDesc("id")));
    }

    @PostMapping
    @Transactional
    public ApiResponse<Address> create(@Valid @RequestBody AddressRequest request) {
        Address address = new Address(); apply(request, address); address.setUserId(CurrentUser.id());
        address.setIsDeleted(0); address.setIsDefault(0); addressMapper.insert(address);
        if (Boolean.TRUE.equals(request.getIsDefault())) setDefault(address);
        return ApiResponse.success("地址已添加", address);
    }

    @GetMapping("/{id}")
    public ApiResponse<Address> get(@PathVariable Long id) { return ApiResponse.success(requireOwned(id)); }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<Address> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        Address address = requireOwned(id); boolean wasDefault = address.getIsDefault() != null && address.getIsDefault() == 1;
        apply(request, address); addressMapper.updateById(address);
        if (Boolean.TRUE.equals(request.getIsDefault())) setDefault(address);
        else if (wasDefault) { address.setIsDefault(1); addressMapper.updateById(address); }
        return ApiResponse.success(address);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Address address = requireOwned(id); address.setIsDeleted(1); addressMapper.updateById(address);
        return ApiResponse.success("地址已删除", null);
    }

    @PutMapping("/{id}/default")
    @Transactional
    public ApiResponse<Void> defaultAddress(@PathVariable Long id) {
        setDefault(requireOwned(id)); return ApiResponse.success("默认地址已更新", null);
    }

    private void setDefault(Address address) {
        Long userId = CurrentUser.id();
        List<Address> addresses = addressMapper.selectList(new QueryWrapper<Address>().eq("user_id", userId).eq("is_deleted", 0));
        for (Address item : addresses) {
            item.setIsDefault(item.getId().equals(address.getId()) ? 1 : 0);
            addressMapper.updateById(item);
        }
    }

    private Address requireOwned(Long id) {
        Address address = addressMapper.selectOne(new QueryWrapper<Address>().eq("id", id)
                .eq("user_id", CurrentUser.id()).eq("is_deleted", 0));
        if (address == null) throw new BusinessException(404, "地址不存在或无权访问");
        return address;
    }

    private void apply(AddressRequest request, Address address) {
        address.setContactName(request.getContactName().trim()); address.setContactPhone(request.getContactPhone().trim());
        address.setRegion(request.getRegion().trim()); address.setDetailAddress(request.getDetailAddress().trim());
        address.setAddressRemark(request.getAddressRemark());
    }

    @Data
    public static class AddressRequest {
        @NotBlank(message = "联系人姓名不能为空") @Size(max = 50) private String contactName;
        @NotBlank(message = "联系电话不能为空") @Size(max = 20) private String contactPhone;
        @NotBlank(message = "所在区域不能为空") @Size(max = 150) private String region;
        @NotBlank(message = "详细地址不能为空") @Size(max = 255) private String detailAddress;
        @Size(max = 255) private String addressRemark;
        private Boolean isDefault;
    }
}
