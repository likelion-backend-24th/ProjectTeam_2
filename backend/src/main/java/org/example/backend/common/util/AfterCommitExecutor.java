package org.example.backend.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
public final class AfterCommitExecutor {

    private AfterCommitExecutor() {}

    /**
     * 트랜잭션이 성공적으로 커밋된 이후에 실행한다.
     * 메일 발송 같은 부가 작업이 본 처리에 영향을 주지 않도록 분리한다.
     */
    public static void run(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("커밋 후 작업 실행 실패", e);
                }
            }
        });
    }
}