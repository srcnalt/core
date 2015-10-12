/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.aurous.jus;


/**
 *
 * @author Aero
 */
public interface UpdateResult {
	public void finished(long time, boolean updated);

	public void failed(long time, Exception exception);
        public void started();
}