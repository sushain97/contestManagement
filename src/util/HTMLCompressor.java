package util;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

public class HTMLCompressor extends HtmlCompressor
{
	public static HTMLCompressor compressor = new HTMLCompressor();
	static
	{
		compressor.setCompressCss(true);
		compressor.setCompressJavaScript(true);
	}
	public static String customCompress(String html)
	{
		return compressor.compress(html);
	}
}