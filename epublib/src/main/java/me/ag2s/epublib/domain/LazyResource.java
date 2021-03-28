package me.ag2s.epublib.domain;

import android.util.Log;

import me.ag2s.epublib.util.IOUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A Resource that loads its data only on-demand from a EPUB book file.
 * This way larger books can fit into memory and can be opened faster.
 */
public class LazyResource extends Resource {

  private static final long serialVersionUID = 5089400472352002866L;
  private  final String TAG= getClass().getName();

  private final LazyResourceProvider resourceProvider;
  private final long cachedSize;

  /**
   * Creates a lazy resource, when the size is unknown.
   *
   * @param resourceProvider The resource provider loads data on demand.
   * @param href The resource's href within the epub.
   */
  public LazyResource(LazyResourceProvider resourceProvider, String href) {
    this(resourceProvider, -1, href);
  }

  /**
   * Creates a Lazy resource, by not actually loading the data for this entry.
   *
   * The data will be loaded on the first call to getData()
   *
   * @param resourceProvider The resource provider loads data on demand.
   * @param size The size of this resource.
   * @param href The resource's href within the epub.
   */
  public LazyResource(
      LazyResourceProvider resourceProvider, long size, String href) {
    super(null, null, href, MediaTypes.determineMediaType(href));
    this.resourceProvider = resourceProvider;
    this.cachedSize = size;
  }

  /**
   * Gets the contents of the Resource as an InputStream.
   *
   * @return The contents of the Resource.
   *
   * @throws IOException IOException
   */
  public InputStream getInputStream() throws IOException {
    if (isInitialized()) {
      return new ByteArrayInputStream(getData());
    } else {
      return resourceProvider.getResourceStream(this.originalHref);
    }
  }

  /**
   * Initializes the resource by loading its data into memory.
   *
   * @throws IOException IOException
   */
  public void initialize() throws IOException {
    getData();
  }

  /**
   * The contents of the resource as a byte[]
   *
   * If this resource was lazy-loaded and the data was not yet loaded,
   * it will be loaded into memory at this point.
   *  This included opening the zip file, so expect a first load to be slow.
   *
   * @return The contents of the resource
   */
  public byte[] getData() throws IOException {

    if (data == null) {

      Log.d(TAG, "Initializing lazy resource: " + this.getHref());

      InputStream in = resourceProvider.getResourceStream(this.originalHref);
      byte[] readData = IOUtil.toByteArray(in, (int) this.cachedSize);
      if (readData == null) {
        throw new IOException(
            "Could not load the contents of resource: " + this.getHref());
      } else {
        this.data = readData;
      }

      in.close();
    }

    return data;
  }

  /**
   * Tells this resource to release its cached data.
   *
   * If this resource was not lazy-loaded, this is a no-op.
   */
  public void close() {
    if (this.resourceProvider != null) {
      this.data = null;
    }
  }

  /**
   * Returns if the data for this resource has been loaded into memory.
   *
   * @return true if data was loaded.
   */
  public boolean isInitialized() {
    return data != null;
  }

  /**
   * Returns the size of this resource in bytes.
   *
   * @return the size.
   */
  public long getSize() {
    if (data != null) {
      return data.length;
    }

    return cachedSize;
  }
}
