/**
 * 
 */
package ftp;

/**
 * @author Jakub Fortunka
 *
 */
public class FTPFile {

	/**
	 * true if file is directory
	 */
	private boolean isDirectory=false;
	/**
	 * stores rights of file (not processed, so it looks like: '-rwxr-xr-x')
	 */
	private String rights;
	/**
	 * name of file
	 */
	private String filename;
	/**
	 * name of owner
	 */
	private String owner;
	/**
	 * name of group
	 */
	private String group;
	/**
	 * size of the file
	 */
	private long size;
	/**
	 * date of last modification
	 */
	private String date;
	
	/**
	 * numeric user rights (0 or 1 or 2 or .. 7)
	 */
	private int userRights;
	/**
	 * numeric group rights (0 or 1 or 2 or .. 7)
	 */
	private int groupRights;
	/**
	 * numeric others rights (0 or 1 or 2 or .. 7)
	 */
	private int othersRights;

	/**
	 * Constructor
	 */
	public FTPFile() {
		filename="..";
		owner=null;
		group=null;
		size=0;
		date=null;
		isDirectory=true;
	}

	/**
	 * Constructor of class that store information about every file on FTP Server. It's pretty simple, but is enough to graphically present content of server directory.
	 * 
	 * @param file String that contains one line returned by the server that is formatted by unix standards (although this is not really standarized, but every server
	 * that i worked with returned LIST in the same way, so i guess this will work everywhere)
	 */
	public FTPFile(String file) {
		boolean filenameHasSpace=false;
		String after = file.replaceAll(" +", " ");
		String[] part = after.split(" ");
		if (part.length>9) {
			filenameHasSpace=true;
		}
		if (part[0].contains("d")) isDirectory=true;
		for (int i = 1;i<part[0].length();i++) {
			if (String.valueOf(part[0].charAt(i)).equals("r")) {
				if (i<4) userRights+=4;
				else if (i<7) groupRights+=4;
				else othersRights+=4;
			}
			if (String.valueOf(part[0].charAt(i)).equals("w")) {
				if (i<4) userRights+=2;
				else if (i<7) groupRights+=2;
				else othersRights+=2;
			}
			if (String.valueOf(part[0].charAt(i)).equals("x")) {
				if (i<4) userRights+=1;
				else if (i<7) groupRights+=1;
				else othersRights+=1;
			}
		}
		rights=part[0];
		owner=part[2];
		group=part[3];
		size = Integer.parseInt(part[4]);
		date = part[5] + " " + part[6] + " " + part[7];
		if (filenameHasSpace) {
			int indexOfBeginOfFilename = file.indexOf(part[8]);
			filename = file.substring(indexOfBeginOfFilename);
		}
		else	filename=part[8];		
	}

	/**
	 * @return the userRights
	 */
	public int getUserRights() {
		return userRights;
	}

	/**
	 * @return the groupRights
	 */
	public int getGroupRights() {
		return groupRights;
	}

	/**
	 * @return the othersRights
	 */
	public int getOthersRights() {
		return othersRights;
	}

	/**
	 * @return the isDirectory
	 */
	public boolean isDirectory() {
		return isDirectory;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}
	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @return the size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @return the date
	 */
	public String getDate() {
		return date;
	}

	/**
	 * @return the rights
	 */
	public String getRights() {
		return rights;
	}

}
