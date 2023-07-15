package me.ag2s.epublib.epub;

import java.util.Objects;

import me.ag2s.epublib.domain.EpubBook;

/**
 * epub导出进度管理器
 *
 * @author Discut
 */
public class EpubWriterProcessor {
    private int totalProgress = 0;
    private int currentProgress = 0;
    private Callback callback;

    public int getCurrentProgress() {
        return currentProgress;
    }

    public int getTotalProgress() {
        return totalProgress;
    }

    public void setTotalProgress(int totalProgress) {
        this.totalProgress = totalProgress;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    protected void updateCurrentProgress(int current) {
        this.currentProgress = Math.min(current, totalProgress);
        if (Objects.isNull(callback)) {
            return;
        }
        callback.onProgressing(totalProgress, this.currentProgress);
    }

    protected Callback getCallback() {
        return callback;
    }

    @SuppressWarnings("unused")
    public interface Callback {
        default void onStart(EpubBook epubBook) {
        }

        default void onProgressing(int total, int progress) {
        }

        default void onEnd(EpubBook epubBook) {
        }

    }
}
