package me.ag2s.epublib.epub;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import me.ag2s.epublib.domain.EpubBook;

/**
 * A book processor that combines several other bookprocessors
 * <p>
 * Fixes coverpage/coverimage.
 * Cleans up the XHTML.
 *
 * @author paul.siegmann
 */
@SuppressWarnings("unused declaration")
public class BookProcessorPipeline implements BookProcessor {

    private static final String TAG = BookProcessorPipeline.class.getName();
    private List<BookProcessor> bookProcessors;

    public BookProcessorPipeline() {
        this(null);
    }

    public BookProcessorPipeline(List<BookProcessor> bookProcessingPipeline) {
        this.bookProcessors = bookProcessingPipeline;
    }

    @Override
    public EpubBook processBook(EpubBook book) {
        if (bookProcessors == null) {
            return book;
        }
        for (BookProcessor bookProcessor : bookProcessors) {
            try {
                book = bookProcessor.processBook(book);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return book;
    }

    public void addBookProcessor(BookProcessor bookProcessor) {
        if (this.bookProcessors == null) {
            bookProcessors = new ArrayList<>();
        }
        this.bookProcessors.add(bookProcessor);
    }

    public void addBookProcessors(Collection<BookProcessor> bookProcessors) {
        if (this.bookProcessors == null) {
            this.bookProcessors = new ArrayList<>();
        }
        this.bookProcessors.addAll(bookProcessors);
    }


    public List<BookProcessor> getBookProcessors() {
        return bookProcessors;
    }


    public void setBookProcessingPipeline(
            List<BookProcessor> bookProcessingPipeline) {
        this.bookProcessors = bookProcessingPipeline;
    }

}
