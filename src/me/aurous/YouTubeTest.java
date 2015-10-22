package me.aurous;

import me.aurous.api.auth.impl.youtube.YouTubeDecoder;

public class YouTubeTest {


	public static void main(String args[]) {
		YouTubeDecoder decoder = new YouTubeDecoder("https://www.youtube.com/watch?v=KEI4qSrkPAs");
		System.out.println(decoder.grab());
	}
	
}
