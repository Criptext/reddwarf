/*
 * Copyright 2007 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.kernel;


/**
 * This is the base interface used for all tasks that can be submitted
 * to the <code>TaskScheduler</code>.
 */
public interface KernelRunnable {

    /**
     * Returns the fully qualified type of the base task that is run by this
     * <code>KernelRunnable</code>. Many types of runnables, for instance
     * <code>TransactionRunner</code>, wrap around other instances of
     * <code>KernelRunnable</code> or <code>Task</code>. This method
     * provides the type of the base task that is being wrapped by any
     * number of <code>KernelRunnable</code>s, where a given task that
     * wraps another task will return that other task's base type
     * such that any wrapping task can be queried and will return the
     * same base task type.
     *
     * @return the fully-qualified name of the base task class type
     */
    public String getBaseTaskType();

    /**
     * Runs this <code>KernelRunnable</code>. If this is run by the
     * <code>TaskScheduler</code>, and if an <code>Exception</code> is
     * thrown that implements <code>ExceptionRetryStatus</code> then
     * the <code>TaskScheduler</code> will consult the
     * <code>shouldRetry</code> method to see if this should be re-run.
     *
     * @throws Exception if any error occurs
     */
    public void run() throws Exception;

}
