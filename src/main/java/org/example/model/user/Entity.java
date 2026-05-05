package org.example.model.user;

import java.io.Serializable;

/**
 * Lớp trừu tượng nền tảng cho tất cả các đối tượng trong hệ thống.
 * Cung cấp thuộc tính định danh (id) chung cho các thực thể.
 */
public abstract class Entity implements Serializable {
    /**
     * Mã định danh duy nhất của thực thể.
     */
    protected String id;

    /**
     * Lấy mã định danh của thực thể.
     *
     * @return chuỗi ID của thực thể.
     */
    public final String getId() {
        return id;
    }

    /**
     * Thiết lập mã định danh cho thực thể.
     *
     * @param id Mã định danh mới cần thiết lập.
     */
    public final void setId(final String id) {
        this.id = id;
    }
}
