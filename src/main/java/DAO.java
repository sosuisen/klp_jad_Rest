import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * DAO for ToDo App
 */
public class DAO {
	private String url;

	public DAO(String url) {
		this.url = url;
		// DriverManger に org.sqlite.JDBC クラス（JDBCドライバ)を登録する処理
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public ToDo get(int id) {
		ToDo todo = null;
		try (
				Connection conn = DriverManager.getConnection(url);
				PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM todo where id=?");
			) {
			pstmt.setInt(1, id);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				int myId = rs.getInt("id");
				String title = rs.getString("title");
				String date = rs.getString("date");
				int completedStr = rs.getInt("completed");
				todo = new ToDo(myId, title, date, completedStr == 1 ? true : false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return todo;
	}

	public ArrayList<ToDo> getAll() {
		var todos = new ArrayList<ToDo>();
		try (
				Connection conn = DriverManager.getConnection(url);
				PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM todo");
			) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				int myId = rs.getInt("id");
				String title = rs.getString("title");
				String date = rs.getString("date");
				int completedStr = rs.getInt("completed");
				todos.add(new ToDo(myId, title, date, completedStr == 1 ? true : false));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return todos;
	}

	public ToDo create(String title, String date, boolean completed) {
		ToDo todo = null;
		try (
				Connection conn = DriverManager.getConnection(url);
				PreparedStatement pstmt = conn
						.prepareStatement("INSERT INTO todo(title, date, completed) VALUES(?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			) {
			pstmt.setString(1, title);
			pstmt.setString(2, date);
			pstmt.setInt(3, completed ? 1: 0);
			pstmt.executeUpdate();

			// AUTOINCREMENTで生成された id を取得します。
			ResultSet rs = pstmt.getGeneratedKeys();
			rs.next();
			int id = rs.getInt(1);

			todo = new ToDo(id, title, date, completed);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return todo;
	}

	public boolean updateTitle(int id, String title) {
		try (
				Connection conn = DriverManager.getConnection(url);
				PreparedStatement pstmt = conn.prepareStatement("UPDATE todo SET title=(?) WHERE id=?");
			) {
			pstmt.setString(1, title);
			pstmt.setInt(2, id);
			int num = pstmt.executeUpdate();
			if (num <= 0) {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean updateDate(int id, String date) {
		try (
				Connection conn = DriverManager.getConnection(url);
				PreparedStatement pstmt = conn.prepareStatement("UPDATE todo SET date=(?) WHERE id=?");
			) {
			pstmt.setString(1, date);
			pstmt.setInt(2, id);
			int num = pstmt.executeUpdate();
			if (num <= 0) {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean updateCompleted(int id, boolean completed) {
		try (
				Connection conn = DriverManager.getConnection(url);
				PreparedStatement pstmt = conn.prepareStatement("UPDATE todo SET completed=(?) WHERE id=?");
			) {
			pstmt.setInt(1, completed ? 1 : 0);
			pstmt.setInt(2, id);
			int num = pstmt.executeUpdate();
			if (num <= 0) {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean delete(int id) {
		try (
				Connection conn = DriverManager.getConnection(url);
				PreparedStatement pstmt = conn.prepareStatement("DELETE from todo WHERE id=?");
			) {
			pstmt.setInt(1, id);
			int num = pstmt.executeUpdate();
			if (num <= 0) {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}
