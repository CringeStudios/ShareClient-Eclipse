package me.mrletsplay.shareclientcore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import me.mrletsplay.shareclientcore.connection.DummyConnection;
import me.mrletsplay.shareclientcore.document.SharedDocument;

public class DocumentTest {

	@Test
	public void testLocalInsert() {
		SharedDocument doc = new SharedDocument(new DummyConnection());
		doc.localInsert(0, "Hello");
		assertEquals("Hello", doc.getContents());
		doc.localInsert(5, " World");
		assertEquals("Hello World", doc.getContents());
		doc.localInsert(5, " Test");
		assertEquals("Hello Test World", doc.getContents());
	}

	@Test
	public void testLocalInsertInvalidIndexFails() {
		SharedDocument doc = new SharedDocument(new DummyConnection());
		doc.localInsert(0, "Hello");
		assertThrows(IllegalArgumentException.class, () -> doc.localInsert(-1, "Test"));
		assertThrows(IllegalArgumentException.class, () -> doc.localInsert(6, "Test"));
	}

	@Test
	public void testLocalDelete() {
		SharedDocument doc = new SharedDocument(new DummyConnection());
		doc.localInsert(0, "Hello World!");
		doc.localDelete(5, 6);
		assertEquals("Hello!", doc.getContents());
	}

	@Test
	public void testLocalDeleteInvalidIndexFails() {
		SharedDocument doc = new SharedDocument(new DummyConnection());
		doc.localInsert(0, "Hello World!");
		assertThrows(IllegalArgumentException.class, () -> doc.localDelete(-1, 10));
		assertThrows(IllegalArgumentException.class, () -> doc.localDelete(12, 1));
		assertThrows(IllegalArgumentException.class, () -> doc.localDelete(0, 13));
	}

}
