import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentServer {
    private List<Student> students;
    private ServerSocket serverSocket;

    public StudentServer() {
        students = new ArrayList<>();
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);

                String request;
                while ((request = reader.readLine()) != null) {
                    String response = processRequest(request);
                    writer.println(response);
                }

                System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
                reader.close();
                writer.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String processRequest(String request) {
            String[] parts = request.split(",");
            String operation = parts[0];

            if (operation.equals("add")) {
                String id = parts[1];
                String name = parts[2];
                int age = Integer.parseInt(parts[3]);

                Student student = new Student(id, name, age);
                students.add(student);
                saveToDatabase();
                return "OK";

            } else if (operation.equals("delete")) {
                String id = parts[1];

                for (Student student : students) {
                    if (student.getId().equals(id)) {
                        students.remove(student);
                        saveToDatabase();
                        return "OK";
                    }
                }

                return "Student not found with ID: " + id;

            } else if (operation.equals("search")) {
                String id = parts[1];

                for (Student student : students) {
                    if (student.getId().equals(id)) {
                        return student.toString();
                    }
                }

                return "Student not found with ID: " + id;

            } else if (operation.equals("update")) {
                String id = parts[1];
                String name = parts[2];
                int age = Integer.parseInt(parts[3]);

                for (Student student : students) {
                    if (student.getId().equals(id)) {
                        student.setName(name);
                        student.setAge(age);
                        saveToDatabase();
                        return "OK";
                    }
                }

                return "Student not found with ID: " + id;
            }

            return "Invalid operation";
        }

        private void saveToDatabase() {
            String url = "jdbc:mysql://localhost:3306/student_db";
            String username = "root";
            String password = "123456";

            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                String sql = "INSERT INTO students (id, name, age) VALUES (?, ?, ?)";
                PreparedStatement statement = conn.prepareStatement(sql);

                // Xóa toàn bộ dữ liệu trong bảng "students"
                String deleteSql = "DELETE FROM students";
                conn.createStatement().executeUpdate(deleteSql);

                // Lưu từng sinh viên vào CSDL
                for (Student student : students) {
                    statement.setString(1, student.getId());
                    statement.setString(2, student.getName());
                    statement.setInt(3, student.getAge());
                    statement.executeUpdate();
                }

                statement.close();
                System.out.println("Data saved to database.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        StudentServer server = new StudentServer();
        server.start(12345);
    }
}