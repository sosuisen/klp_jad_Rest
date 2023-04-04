import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@WebServlet(urlPatterns = { "/todos/*" })
/**
 * /todos が呼ばれたとき、 request.getPathInfo() の値は null
 * /todos/ が呼ばれたとき、request.getPathInfo() の値は ""
 * /todos/3 : request.getPathInfo() の値は "/3"
 */
public class Resources extends HttpServlet {
	/**
	 * Servletでは、配置後のプロジェクトは、開発中のプロジェクトフォルダとは別の場所へ移動します。
	 * 環境によって異なりますが、たとえば
	 * C:\pleiades-jad2023\workspace\.metadata\.plugins\org.eclipse.wst.server.core\tmp0\wtpwebapps\Rest\
	 * データベースの場所は、フルパスで指定しましょう。
	 */
	private String dbPath = "c:\\pleiades-jad2023\\workspace\\Rest\\jad.db";
	private final DAO dao = new DAO("jdbc:sqlite:" + dbPath);
	private Gson gson = new Gson();
	private Type todosType = new TypeToken<ArrayList<ToDo>>() {
	}.getType();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = response.getWriter();
		var path = request.getPathInfo();
		if (path == null) {
			// URL が /todos の場合。 
			// メンバー一覧を返す
			out.print(gson.toJson(dao.getAll(), todosType));
			// なお、outは Tomcat が close() してくれるので自分で閉じなくてもよい。
			return;
		}

		if (path.matches("^/\\d+$")) {
			// URL が /todos/3 のようなパターンとマッチした場合。
			//　この場合、pathは /3
			path = path.substring(1); // 先頭の1文字（/）を削除
			var todo = dao.get(Integer.parseInt(path));
			if (todo != null) {
				out.print(gson.toJson(todo, ToDo.class));
				return;
			}
		}

		// それ以外
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/json;charset=UTF-8");

		var title = request.getParameter("title");

		if (title == null) {
			// 必須パラメータが足りない場合は 400 Bad Request
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		var date = LocalDate.now().toString();

		var todo = dao.create(title, date, false);

		PrintWriter out = response.getWriter();
		out.print(gson.toJson(todo, ToDo.class));
	}

	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/json;charset=UTF-8");

		var path = request.getPathInfo();
		if (!path.matches("^/\\d+$")) {
			// URL が /todos/3 のようなパターンとマッチしない場合。
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		path = path.substring(1); // 先頭の1文字（/）を削除
		var id = Integer.parseInt(path);

		// Tomcat では doPut, doDelete において
		// request.getParameter() を用いたパラメータ取得ができない。
		// よって、受信したデータを手動でパースする。
		var br = new BufferedReader(new InputStreamReader(request.getInputStream()));
		var queryString = br.readLine();
		Map<String, String> map = parseQuery(queryString);

		var title = map.get("title");
		var date = map.get("date");
		var completedParam = map.get("completed");

		// ここから発展課題
		var exists = true;
		if (title != null) {
			exists = dao.updateTitle(id, title);
		}

		if (exists && date != null) {
			exists = dao.updateDate(id, date);
		}

		if (exists && completedParam != null) {
			var completed = Boolean.parseBoolean(completedParam);
			exists = dao.updateCompleted(id, completed);
		}

		if (!exists) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		PrintWriter out = response.getWriter();
		out.print(gson.toJson(dao.get(id), ToDo.class));
	}

	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json;charset=UTF-8");

		var path = request.getPathInfo();
		if (!path.matches("^/\\d+$")) {
			// URL が /todos/3 のようなパターンとマッチしない場合。
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		path = path.substring(1); // 先頭の1文字（/）を削除
		var id = Integer.parseInt(path);

		// ここから基本課題
		var deletedTodo = dao.get(id);
		if (deletedTodo == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		dao.delete(id);

		PrintWriter out = response.getWriter();
		out.print(gson.toJson(deletedTodo, ToDo.class));
	}

	private Map<String, String> parseQuery(String query) {
		var params = query.split("&");
		var map = new HashMap<String, String>();
		for (var param : params) {
			var val = param.split("=");
			try {
				if (val.length == 2) {
					map.put(URLDecoder.decode(val[0], "utf-8"), URLDecoder.decode(val[1], "utf-8"));
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return map;
	}
}
