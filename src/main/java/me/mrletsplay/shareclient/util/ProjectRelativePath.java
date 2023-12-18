package me.mrletsplay.shareclient.util;

import java.util.Objects;

public record ProjectRelativePath(String projectName, String relativePath) {

	public ProjectRelativePath {
		Objects.requireNonNull(projectName, "projectName must not be null");
		Objects.requireNonNull(relativePath, "relativePath must not be null");
	}

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
