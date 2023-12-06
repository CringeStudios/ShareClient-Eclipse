package me.mrletsplay.shareclient.util;

public record ProjectRelativePath(String projectName, String relativePath) {

	@Override
	public String toString() {
		return projectName + ":" + relativePath;
	}

	public static ProjectRelativePath of(String raw) {
		String[] spl = raw.split(":", 2);
		if(spl.length != 2) throw new IllegalArgumentException("Invalid path");
		return new ProjectRelativePath(spl[0], spl[1]);
	}

}
