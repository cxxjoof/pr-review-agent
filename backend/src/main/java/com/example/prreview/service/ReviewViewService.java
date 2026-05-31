package com.example.prreview.service;

import com.example.prreview.enums.PrType;
import com.example.prreview.enums.ResultViewType;
import org.springframework.stereotype.Service;

@Service
public class ReviewViewService {

    public ResultViewType resolveResultViewType(PrType prType) {
        if (prType == null) {
            return ResultViewType.REVIEW_FINDINGS;
        }

        return switch (prType) {
            case DOCUMENTATION -> ResultViewType.DOCUMENTATION_FINDINGS;
            case CODE, MIXED -> ResultViewType.CODE_RISKS;
            case CONFIG, TEST, DEPENDENCY, CICD -> ResultViewType.REVIEW_FINDINGS;
        };
    }
}
