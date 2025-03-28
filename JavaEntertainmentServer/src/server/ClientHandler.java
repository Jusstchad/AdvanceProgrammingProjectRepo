package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JOptionPane;

import domain.Customer;
import domain.Equipment;
import domain.Rent;

public class ClientHandler extends Thread {

    private Socket connectionSocket;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private Connection mycon;
    private PreparedStatement stat;
    private ResultSet results;

    public ClientHandler(Socket socket) {
        this.connectionSocket = socket;
        this.mycon = Server.getDatabaseConnection(); // Get connection from Server class
    }

    private void configureStreams() {
        try {
            os = new ObjectOutputStream(connectionSocket.getOutputStream());
            is = new ObjectInputStream(connectionSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnections() {
        try {
            if (is != null) is.close();
            if (os != null) os.close();
            if (connectionSocket != null) connectionSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addCustomerToDb(Customer cus) {
        String sql = "INSERT INTO CUSTOMER (cusID, name, dOB, age, address, email, phoneNumber, gender) VALUES(?,?,?,?,?,?,?,?)";
        try {
            stat = mycon.prepareStatement(sql);
            stat.setString(1, cus.getCusID());
            stat.setString(2, cus.getName());
            stat.setDate(3, java.sql.Date.valueOf(cus.getdOB()));
            stat.setInt(4, cus.getAge());
            stat.setString(5, cus.getAddress());
            stat.setString(6, cus.getEmail());
            stat.setString(7, cus.getPhoneNumber());
            stat.setString(8, String.valueOf(cus.getGender()));

            int rowsAdded = stat.executeUpdate();
            if (rowsAdded > 0) {
                System.out.println("Customer added successfully.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addEquipmentToDb(Equipment equip) {
        String sql = "INSERT INTO EQUIPMENT(equipId, name, type, status, cost, amount) VALUES(?,?,?,?,?,?)";
        try {
            stat = mycon.prepareStatement(sql);
            stat.setString(1, equip.getEquipId()); 
            stat.setString(2, equip.getName());     
            stat.setString(3, equip.getType());    
            stat.setString(4, equip.getStatus());   
            stat.setDouble(5, equip.getAmount());  
            stat.setDouble(6, equip.getCost());    
            
            int rowsAdded = stat.executeUpdate();
            if (rowsAdded > 0) {
                System.out.println("Equipment added successfully.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void addRentsToDb(Rent rent) {
        String sql = "INSERT INTO RENTS (customerId, equipmentId, amount_owed, rent_date, rent_time, cost, amount_paid, duration, status, " +
                     "date_day, date_month, date_year, time_hours, time_minutes, time_seconds, " +
                     "duration_hours, duration_minutes, duration_seconds) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try {
            stat = mycon.prepareStatement(sql);
  
            stat.setString(1, rent.getCustomerId());
            stat.setString(2, rent.getEquipmentId());
            stat.setDouble(3, rent.getAmountOwed());
            stat.setString(4, rent.getDate().toString()); 
            stat.setString(5, rent.getTime().toString()); 
            stat.setDouble(6, rent.getCost());
            stat.setDouble(7, rent.getAmountPaid());
            stat.setString(8, rent.getDuration().toString()); 
            stat.setString(9, rent.getStatus());
            
            // Decompose date components
            stat.setInt(10, rent.getDate().getDay());
            stat.setInt(11, rent.getDate().getMonth());
            stat.setInt(12, rent.getDate().getYear());
            
            // Decompose time components
            stat.setInt(13, rent.getTime().getHours());
            stat.setInt(14, rent.getTime().getMinutes());
            stat.setInt(15, rent.getTime().getSeconds());
            
            // Decompose duration components
            stat.setInt(16, rent.getDuration().getHours());
            stat.setInt(17, rent.getDuration().getMinutes());
            stat.setInt(18, rent.getDuration().getSeconds());
            
            int rowsAdded = stat.executeUpdate();
            if (rowsAdded > 0) {
                System.out.println("Rent added successfully.");
                os.writeObject("Rent added successfully."); 
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                os.writeObject("Error adding rent: " + e.getMessage()); 
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void run() {
        configureStreams();
        try {
            String action = (String) is.readObject();

            switch (action) {
                case "add customer":
                    Customer cus = (Customer) is.readObject();
                    addCustomerToDb(cus);
                    break;
                case "add equipment":
                    Equipment equip = (Equipment) is.readObject();
                    addEquipmentToDb(equip);
                    break;
                case "add rent":
                    Rent rent = (Rent) is.readObject();
                    addRentsToDb(rent);
                    break;
                default:
                    System.out.println("Unknown action: " + action);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }
}
