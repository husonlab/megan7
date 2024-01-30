/*
 * IService.java Copyright (C) 2024 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package megan.ms.clientdialog.service;

import java.io.FileFilter;
import java.util.List;

/**
 * Represents a directory system of files, can be local or on a server
 * <p/>
 * Created by huson on 10/3/14.
 */
interface IService {
	/**
	 * get URL of remote server
	 *
	 * @return url
	 */
	String getRemoteURL();

	/**
	 * get the local name of this node
	 *
	 * @return local name
	 */
	String getNodeName();

	/**
	 * set the file filter
	 */
	void setFileFilter(FileFilter fileFilter);

	/**
	 * is this node currently available?
	 *
	 * @return availability
	 */
	boolean isAvailable();

	/**
	 * get a list of available files and their unique ids
	 *
	 * @return list of available files in format path,id
	 */
	List<String> getAvailableFiles();

	/**
	 * gets the file length for the given file
	 *
	 * @return file length
	 */
	long getFileLength(int fileId);

	/**
	 * opens the specified file for reading
	 *
	 * @return handle id
	 */
	int openFile(String name);

	/**
	 * seek
	 */
	void seek(int handleId, long pos);

	/**
	 * read the specified number of bytes
	 *
	 * @param handleId value returned by openFile
	 * @return number of bytes read
	 */
	int read(int handleId, byte[] buffer, int offset, int length);

	/**
	 * close the file associated with the given handle
	 */
	void closeFile(int handleId);

	/**
	 * gets the last time that the content of this node was updated
	 *
	 * @return last rescan time
	 */
	long getLastUpdateTime();

	/**
	 * get the file id for a name
	 *
	 * @return file id
	 */
	Integer getId(String name);

	/**
	 * get current position in file
	 *
	 * @return current position
	 */
	long getPosition(int handleId);
}
