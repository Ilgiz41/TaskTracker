package org.example.util;

import lombok.Getter;
import org.example.datasource.repositoryservice.RegularTaskRepositoryService;

public class RegularTaskRepositoryUtil {

    @Getter
    private static final RegularTaskRepositoryService regularTaskRepositoryService;

    static {
        regularTaskRepositoryService = new RegularTaskRepositoryService();
    }

}
