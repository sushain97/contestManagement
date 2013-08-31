/* Component of GAE Project for Dulles TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]. 
 */

package util;

import java.io.StringWriter;

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
	public static String customCompress(StringWriter sw)
	{
		return compressor.compress(sw.toString());
	}
}