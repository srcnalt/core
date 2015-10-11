/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.aurous;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import me.aurous.jus.DynamicUpdater;

/**
 *
 * @author Aero
 */
public class JUUS { // Java Userlevel Update System

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(final String[] args) {
		final DynamicUpdater dup = new DynamicUpdater();
		dup.setUpdateDirectory(new File("./updates/"));

		// to update hash list file
		try {
			dup.rebase(new File("./update.json"));
		} catch (NoSuchAlgorithmException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// register volatile files
		dup.addNonWritable("Aurous.jar");

		/*
		 * dup.update("http://192.168.1.110:51113/", new UpdateResult() {
		 * 
		 * @Override public void finished(long time) {
		 * System.out.println("Update finished"); }
		 * 
		 * @Override public void failed(long time, Exception exception) {
		 * System.out.println("Update failed!"); exception.printStackTrace(); }
		 * 
		 * });
		 */

	}

}