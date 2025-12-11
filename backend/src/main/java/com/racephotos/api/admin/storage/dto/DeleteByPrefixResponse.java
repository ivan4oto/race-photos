package com.racephotos.api.admin.storage.dto;

public record DeleteByPrefixResponse(
        long deletedS3Objects,
        long deletedPhotoAssets
) {
    public static DeleteByPrefixResponse from(long deletedS3Objects, long deletedPhotoAssets) {
        return new DeleteByPrefixResponse(deletedS3Objects, deletedPhotoAssets);
    }
}
