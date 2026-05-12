package com.fivefy.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface AudioStorageService {

    String upload(MultipartFile audioFile);
}
