package me.aurous;

import me.aurous.api.auth.impl.youtube.YouTubeDecoder;
import me.aurous.utils.Internet;

public class YouTubeTest {


	public static void main(String args[]) {
		System.out.println(Internet.text("https://www.youtube.com/results?search_query=look+on+down+from+the+bridge+mazzy+star&filters=video&lclk=video"));
		YouTubeDecoder decoder = new YouTubeDecoder("https://www.youtube.com/watch?v=p3NZn0mA_XI");
		System.out.println(decoder.grab());
	}
	
}
