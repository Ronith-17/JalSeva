import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;
public class JalSevaSystem {
 //
======================================================================
===
 // 1. INTERFACES & DOMAIN MODEL (Units I - III)
 //
======================================================================
===
 public interface Chargeable {
 double charge(int litres);
 boolean isFree();
 String invoice(int litres);
 }
 public static abstract class WaterConsumer {
 private int consumerId;
 protected String name;
 protected int ward;
 public WaterConsumer(int consumerId, String name, int ward) {
 this.consumerId = consumerId;
 this.name = name;
 this.ward = ward;
 }
 public int getConsumerId() { return consumerId; }
 public String getName() { return name; }
 public int getWard() { return ward; }
 public abstract int priorityLevel();
 public final String requestNumber(int reqId) {
 return "WR-" + ward + String.format("%04d", reqId);
 }
 @Override
 public boolean equals(Object o) {
 if (this == o) return true;
 if (!(o instanceof WaterConsumer)) return false;
 WaterConsumer that = (WaterConsumer) o;
 return consumerId == that.consumerId;
 }
 @Override
 public int hashCode() {
 return Objects.hash(consumerId);
 }
 @Override
 public String toString() {
 return "[" + consumerId + "] " + name + " (Ward " + ward + ", Priority " +
priorityLevel() + ")";
 }
 }
 public static class ResidentialConsumer extends WaterConsumer implements
Chargeable {
 public ResidentialConsumer(int consumerId, String name, int ward) {
 super(consumerId, name, ward);
 }
 @Override public int priorityLevel() { return 2; }
 @Override public double charge(int litres) { return 0.0; }
 @Override public boolean isFree() { return true; }
 @Override
 public String invoice(int litres) {
 return "Residential Water Supply: FREE under Municipal Scheme (" + litres + "L)";
 }
 }
 public static class CommercialConsumer extends WaterConsumer implements
Chargeable {
 public static final double RATE_PER_LITRE = 0.15; // Rs. 0.15 per litre
 public CommercialConsumer(int consumerId, String name, int ward) {
 super(consumerId, name, ward);
 }
 @Override public int priorityLevel() { return 3; }
 @Override public double charge(int litres) { return litres * RATE_PER_LITRE; }
 @Override public boolean isFree() { return false; }
 @Override
 public String invoice(int litres) {
 return String.format("Commercial Invoice: %dL @ Rs. %.2f/L = Rs. %.2f", litres,
RATE_PER_LITRE, charge(litres));
 }
 }
 public static class InstitutionalConsumer extends WaterConsumer implements
Chargeable {
 private String institutionType;
 public InstitutionalConsumer(int consumerId, String name, int ward, String
institutionType) {
 super(consumerId, name, ward);
 this.institutionType = institutionType;
 }
 public String getInstitutionType() { return institutionType; }
 @Override public int priorityLevel() { return 2; }
 @Override public double charge(int litres) { return litres * 0.05; }
 @Override public boolean isFree() { return false; }
 @Override
 public String invoice(int litres) {
 return "Institutional Invoice (" + institutionType + "): " + litres + "L = Rs. " +
charge(litres);
 }
 }
 public static final class HospitalConsumer extends InstitutionalConsumer {
 private int bedCount;
 public HospitalConsumer(int consumerId, String name, int ward, int bedCount) {
 super(consumerId, name, ward, "Hospital");
 this.bedCount = bedCount;
 }
 public int getBedCount() { return bedCount; }
 @Override public int priorityLevel() { return 1; } // Highest Priority
 @Override public double charge(int litres) { return 0.0; }
 @Override public boolean isFree() { return true; }
 @Override
 public String invoice(int litres) {
 return "Hospital Emergency Supply (" + bedCount + " beds): 100% Free / Highest
Priority";
 }
 }
 public static class Tanker {
 public static final int TANKER_CAPACITY_LITRES = 12000;
 private String regNo;
 private int capacity;
 private String driver;
 private String status;
 public Tanker(String regNo) {
 this(regNo, TANKER_CAPACITY_LITRES, "Unassigned");
 }
 public Tanker(String regNo, int capacity) {
 this(regNo, capacity, "Unassigned");
 }
 public Tanker(String regNo, int capacity, String driver) {
 this.regNo = regNo;
 this.capacity = capacity;
 this.driver = driver;
 this.status = "FREE";
 }
 public String getRegNo() { return regNo; }
 public int getCapacity() { return capacity; }
 public String getDriver() { return driver; }
 public String getStatus() { return status; }
 public void setStatus(String status) { this.status = status; }
 @Override
 public String toString() {
 return regNo + " [" + capacity + "L] - Driver: " + driver + " (" + status + ")";
 }
 }
 public static class WaterRequest {
 private int requestId;
 private int consumerId;
 private int litres;
 private String urgency;
 private String status;
 public WaterRequest(int requestId, int consumerId, int litres, String urgency, String
status) {
 this.requestId = requestId;
 this.consumerId = consumerId;
 this.litres = litres;
 this.urgency = urgency;
 this.status = status;
 }
 public int getRequestId() { return requestId; }
 public int getConsumerId() { return consumerId; }
 public int getLitres() { return litres; }
 public String getUrgency() { return urgency; }
 public String getStatus() { return status; }
 public void setStatus(String status) { this.status = status; }
 @Override
 public String toString() {
 return "REQ-" + requestId + " (Consumer: " + consumerId + ", " + litres + "L, " +
urgency + ", Status: " + status + ")";
 }
 }
 //
======================================================================
===
 // 2. USER-DEFINED EXCEPTIONS (Unit II)
 //
======================================================================
===
 public static class JalSevaException extends Exception {
 public JalSevaException(String msg) { super(msg); }
 }
 public static class NoTankerAvailableException extends JalSevaException {
 public NoTankerAvailableException(String msg) { super(msg); }
 }
 public static class QuotaExceededException extends JalSevaException {
 public QuotaExceededException(String msg) { super(msg); }
 }
 public static class InvalidWardException extends JalSevaException {
 public InvalidWardException(String msg) { super(msg); }
 }
 //
======================================================================
===
 // 3. DATABASE ACCESS OBJECT (Hybrid SQLite with Resilient In-Memory Fallback)
 //
======================================================================
===
 public static class JalSevaDAO {
 private static final String URL = "jdbc:sqlite:watertanker.db";
 private boolean sqliteAvailable = false;
 // In-memory fallback stores if SQLite JDBC jar is absent
 private final List<ConsumerRecord> memConsumers = new ArrayList<>();
 private final List<Tanker> memTankers = new ArrayList<>();
 private final List<RequestRecord> memRequests = new ArrayList<>();
 private final List<TripRecord> memTrips = new ArrayList<>();
 private int nextReqId = 21;
 private int nextTripId = 21;
 public static class ConsumerRecord {
 int id; String name; String type; int ward; String phone;
 public ConsumerRecord(int id, String name, String type, int ward, String phone) {
 this.id = id; this.name = name; this.type = type; this.ward = ward; this.phone =
phone;
 }
 }
 public static class RequestRecord {
 int reqId; int cid; int litres; String status; String time;
 public RequestRecord(int reqId, int cid, int litres, String status, String time) {
 this.reqId = reqId; this.cid = cid; this.litres = litres; this.status = status; this.time
= time;
 }
 }
 public static class TripRecord {
 int tripId; int reqId; String regNo; String dispatchTime; String deliverTime; String
status;
 public TripRecord(int tripId, int reqId, String regNo, String dispatchTime, String
deliverTime, String status) {
 this.tripId = tripId; this.reqId = reqId; this.regNo = regNo;
 this.dispatchTime = dispatchTime; this.deliverTime = deliverTime; this.status =
status;
 }
 }
 public JalSevaDAO() {
 try {
 Class.forName("org.sqlite.JDBC");
 try (Connection conn = DriverManager.getConnection(URL)) {
 sqliteAvailable = true;
 initSchema(conn);
 seedInitialRecords(conn);
 }
 } catch (Exception e) {
 sqliteAvailable = false;
 seedInMemoryRecords();
 }
 }
 public boolean isSqliteAvailable() {
 return sqliteAvailable;
 }
 private void initSchema(Connection conn) throws SQLException {
 try (Statement st = conn.createStatement()) {
 st.execute("CREATE TABLE IF NOT EXISTS consumers (" +
 "consumer_id INTEGER PRIMARY KEY, " +
 "name TEXT NOT NULL, " +
 "type TEXT NOT NULL, " +
 "ward INTEGER NOT NULL, " +
 "phone TEXT)");
 st.execute("CREATE TABLE IF NOT EXISTS tankers (" +
 "reg_no TEXT PRIMARY KEY, " +
 "capacity INTEGER NOT NULL, " +
 "driver TEXT NOT NULL, " +
 "status TEXT NOT NULL)");
 st.execute("CREATE TABLE IF NOT EXISTS requests (" +
 "request_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
 "consumer_id INTEGER NOT NULL, " +
 "litres INTEGER NOT NULL, " +
 "status TEXT NOT NULL, " +
 "logged_time TEXT NOT NULL)");
 st.execute("CREATE TABLE IF NOT EXISTS trips (" +
 "trip_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
 "request_id INTEGER NOT NULL, " +
 "reg_no TEXT NOT NULL, " +
 "dispatched_time TEXT NOT NULL, " +
 "delivered_time TEXT, " +
 "status TEXT NOT NULL)");
 }
 }
 private void seedInitialRecords(Connection conn) throws SQLException {
 try (Statement st = conn.createStatement()) {
 ResultSet rsC = st.executeQuery("SELECT COUNT(*) FROM consumers");
 if (rsC.next() && rsC.getInt(1) < 20) {
 String[] names = getSampleNames();
 String[] types = getSampleTypes();
 String sql = "INSERT OR IGNORE INTO consumers VALUES (?, ?, ?, ?, ?)";
 try (PreparedStatement ps = conn.prepareStatement(sql)) {
 for (int i = 0; i < 20; i++) {
 ps.setInt(1, 101 + i);
 ps.setString(2, names[i]);
 ps.setString(3, types[i]);
 ps.setInt(4, (i % 20) + 1);
 ps.setString(5, "9876500" + String.format("%02d", 10 + i));
 ps.executeUpdate();
 }
 }
 }
 ResultSet rsT = st.executeQuery("SELECT COUNT(*) FROM tankers");
 if (rsT.next() && rsT.getInt(1) < 20) {
 String[] drivers = getSampleDrivers();
 String sql = "INSERT OR IGNORE INTO tankers VALUES (?, ?, ?, 'FREE')";
 try (PreparedStatement ps = conn.prepareStatement(sql)) {
 for (int i = 0; i < 20; i++) {
 String reg = String.format("AP16 TT %04d", 4401 + i);
 int cap = (i % 3 == 0) ? 15000 : 12000;
 ps.setString(1, reg);
 ps.setInt(2, cap);
 ps.setString(3, drivers[i]);
 ps.executeUpdate();
 }
 }
 }
 ResultSet rsR = st.executeQuery("SELECT COUNT(*) FROM requests");
 if (rsR.next() && rsR.getInt(1) < 20) {
 String sql = "INSERT INTO requests (consumer_id, litres, status, logged_time)
VALUES (?, ?, 'PENDING', '2026-11-02 08:00:00')";
 try (PreparedStatement ps = conn.prepareStatement(sql)) {
 for (int i = 0; i < 20; i++) {
 ps.setInt(1, 101 + (i % 20));
 ps.setInt(2, 5000 + (i * 500));
 ps.executeUpdate();
 }
 }
 }
 ResultSet rsTr = st.executeQuery("SELECT COUNT(*) FROM trips");
 if (rsTr.next() && rsTr.getInt(1) < 20) {
 String sql = "INSERT INTO trips (request_id, reg_no, dispatched_time,
delivered_time, status) VALUES (?, ?, ?, ?, 'DELIVERED')";
 try (PreparedStatement ps = conn.prepareStatement(sql)) {
 for (int i = 1; i <= 20; i++) {
 String reg = String.format("AP16 TT %04d", 4401 + (i % 20));
 ps.setInt(1, i);
 ps.setString(2, reg);
 ps.setString(3, "2026-11-02 09:" + String.format("%02d", (i * 2)) + ":00");
 ps.setString(4, "2026-11-02 10:" + String.format("%02d", (i * 2)) + ":00");
 ps.executeUpdate();
 }
 }
 }
 }
 }
 private void seedInMemoryRecords() {
 String[] names = getSampleNames();
 String[] types = getSampleTypes();
 String[] drivers = getSampleDrivers();
 for (int i = 0; i < 20; i++) {
 memConsumers.add(new ConsumerRecord(101 + i, names[i], types[i], (i % 20)
+ 1, "9876500" + (10 + i)));
 String reg = String.format("AP16 TT %04d", 4401 + i);
 int cap = (i % 3 == 0) ? 15000 : 12000;
 memTankers.add(new Tanker(reg, cap, drivers[i]));
 memRequests.add(new RequestRecord(i + 1, 101 + (i % 20), 5000 + (i * 500),
"PENDING", "2026-11-02 08:00:00"));
 memTrips.add(new TripRecord(i + 1, i + 1, reg, "2026-11-02 09:" +
String.format("%02d", i * 2) + ":00",
 "2026-11-02 10:" + String.format("%02d", i * 2) + ":00", "DELIVERED"));
 }
 }
 private String[] getSampleNames() {
 return new String[]{
 "Apex Govt Hospital", "City Care Hospital", "Sunshine Hospital", "LifeLine Trauma
Center",
 "Greenwood Colony", "Lakeview Apartments", "Sunrise Heights", "Tulsi Enclave",
 "Shanti Nagar", "Lotus Boulevard", "Metro Beverages", "Apex Ice Factory",
 "Deccan Bottlers", "Royal Star Hotel", "Grand Plaza Mall", "City Mall",
 "Apex Engineering College", "City Polytechnic", "Vidya Bhavan High School",
"National Public School"
 };
 }
 private String[] getSampleTypes() {
 return new String[]{
 "Hospital", "Hospital", "Hospital", "Hospital",
 "Residential", "Residential", "Residential", "Residential",
 "Residential", "Residential", "Commercial", "Commercial",
 "Commercial", "Commercial", "Commercial", "Commercial",
 "Institutional", "Institutional", "Institutional", "Institutional"
 };
 }
 private String[] getSampleDrivers() {
 return new String[]{
 "Ramu", "Suresh", "Mahesh", "Ganesh", "Vijay", "Anand", "Rajesh", "Kishore",
 "Praveen", "Dinesh", "Manoj", "Naresh", "Sunil", "Pradeep", "Satish", "Mohan",
 "Kiran", "Sanjay", "Ravi", "Balaji"
 };
 }
 public synchronized void insertConsumer(int id, String name, String type, int ward,
String phone) throws SQLException {
 if (sqliteAvailable) {
 String sql = "INSERT OR REPLACE INTO consumers (consumer_id, name, type,
ward, phone) VALUES (?, ?, ?, ?, ?)";
 try (Connection conn = DriverManager.getConnection(URL);
 PreparedStatement ps = conn.prepareStatement(sql)) {
 ps.setInt(1, id); ps.setString(2, name); ps.setString(3, type); ps.setInt(4, ward);
ps.setString(5, phone);
 ps.executeUpdate();
 }
 } else {
 memConsumers.removeIf(c -> c.id == id);
 memConsumers.add(new ConsumerRecord(id, name, type, ward, phone));
 }
 }
 public synchronized int insertRequest(int consumerId, int litres, String time) throws
SQLException {
 if (sqliteAvailable) {
 String sql = "INSERT INTO requests (consumer_id, litres, status, logged_time)
VALUES (?, ?, 'PENDING', ?)";
 try (Connection conn = DriverManager.getConnection(URL);
 PreparedStatement ps = conn.prepareStatement(sql,
Statement.RETURN_GENERATED_KEYS)) {
 ps.setInt(1, consumerId); ps.setInt(2, litres); ps.setString(3, time);
 ps.executeUpdate();
 ResultSet rs = ps.getGeneratedKeys();
 return rs.next() ? rs.getInt(1) : -1;
 }
 } else {
 int reqId = nextReqId++;
 memRequests.add(new RequestRecord(reqId, consumerId, litres, "PENDING",
time));
 return reqId;
 }
 }
 public synchronized void insertTanker(String regNo, int capacity, String driver)
throws SQLException {
 if (sqliteAvailable) {
 String sql = "INSERT OR REPLACE INTO tankers (reg_no, capacity, driver, status)
VALUES (?, ?, ?, 'FREE')";
 try (Connection conn = DriverManager.getConnection(URL);
 PreparedStatement ps = conn.prepareStatement(sql)) {
 ps.setString(1, regNo); ps.setInt(2, capacity); ps.setString(3, driver);
 ps.executeUpdate();
 }
 } else {
 memTankers.removeIf(t -> t.getRegNo().equalsIgnoreCase(regNo));
 memTankers.add(new Tanker(regNo, capacity, driver));
 }
 }
 public synchronized int insertTrip(int reqId, String regNo, String time) throws
SQLException {
 if (sqliteAvailable) {
 String sql = "INSERT INTO trips (request_id, reg_no, dispatched_time, status)
VALUES (?, ?, ?, 'ON_TRIP')";
 try (Connection conn = DriverManager.getConnection(URL);
 PreparedStatement ps = conn.prepareStatement(sql,
Statement.RETURN_GENERATED_KEYS)) {
 ps.setInt(1, reqId); ps.setString(2, regNo); ps.setString(3, time);
 ps.executeUpdate();
 ResultSet rs = ps.getGeneratedKeys();
 return rs.next() ? rs.getInt(1) : -1;
 }
 } else {
 int tripId = nextTripId++;
 memTrips.add(0, new TripRecord(tripId, reqId, regNo, time, null, "ON_TRIP"));
 return tripId;
 }
 }
 public synchronized void updateTankerStatus(String regNo, String status) throws
SQLException {
 if (sqliteAvailable) {
 String sql = "UPDATE tankers SET status = ? WHERE reg_no = ?";
 try (Connection conn = DriverManager.getConnection(URL);
 PreparedStatement ps = conn.prepareStatement(sql)) {
 ps.setString(1, status); ps.setString(2, regNo);
 ps.executeUpdate();
 }
 } else {
 for (Tanker t : memTankers) {
 if (t.getRegNo().equalsIgnoreCase(regNo)) {
 t.setStatus(status);
 break;
 }
 }
 }
 }
 public synchronized void updateRequestStatus(int reqId, String status) throws
SQLException {
 if (sqliteAvailable) {
 String sql = "UPDATE requests SET status = ? WHERE request_id = ?";
 try (Connection conn = DriverManager.getConnection(URL);
 PreparedStatement ps = conn.prepareStatement(sql)) {
 ps.setString(1, status); ps.setInt(2, reqId);
 ps.executeUpdate();
 }
 } else {
 for (RequestRecord r : memRequests) {
 if (r.reqId == reqId) {
 r.status = status;
 break;
 }
 }
 }
 }
 public synchronized void completeTrip(int tripId, String deliveredTime) throws
SQLException {
 if (sqliteAvailable) {
 String sql = "UPDATE trips SET status = 'DELIVERED', delivered_time = ? WHERE
trip_id = ?";
 try (Connection conn = DriverManager.getConnection(URL);
 PreparedStatement ps = conn.prepareStatement(sql)) {
 ps.setString(1, deliveredTime); ps.setInt(2, tripId);
 ps.executeUpdate();
 }
 } else {
 for (TripRecord tr : memTrips) {
 if (tr.tripId == tripId) {
 tr.status = "DELIVERED";
 tr.deliverTime = deliveredTime;
 break;
 }
 }
 }
 }
 public List<Tanker> getAllTankers() throws SQLException {
 if (sqliteAvailable) {
 List<Tanker> list = new ArrayList<>();
 try (Connection conn = DriverManager.getConnection(URL);
 Statement st = conn.createStatement();
 ResultSet rs = st.executeQuery("SELECT * FROM tankers")) {
 while (rs.next()) {
 Tanker t = new Tanker(rs.getString("reg_no"), rs.getInt("capacity"),
rs.getString("driver"));
 t.setStatus(rs.getString("status"));
 list.add(t);
 }
 }
 return list;
 } else {
 return new ArrayList<>(memTankers);
 }
 }
 public List<String> getTripDisplayList() throws SQLException {
 List<String> result = new ArrayList<>();
 if (sqliteAvailable) {
 try (Connection conn = DriverManager.getConnection(URL);
 Statement st = conn.createStatement();
 ResultSet rs = st.executeQuery("SELECT t.trip_id, t.reg_no, t.dispatched_time,
t.status, c.name, c.ward " +
 "FROM trips t JOIN requests r ON t.request_id = r.request_id " +
 "JOIN consumers c ON r.consumer_id = c.consumer_id ORDER BY t.trip_id
DESC")) {
 while (rs.next()) {
 result.add("Trip #" + rs.getInt("trip_id") + " | " + rs.getString("reg_no") +
 " | " + rs.getString("name") + " (Ward " + rs.getInt("ward") + ") | " +
 rs.getString("dispatched_time") + " | [" + rs.getString("status") + "]");
 }
 }
 } else {
 for (TripRecord tr : memTrips) {
 String cName = "Consumer " + tr.reqId;
 int ward = (tr.reqId % 20) + 1;
 for (ConsumerRecord cr : memConsumers) {
 if (cr.id == (101 + (tr.reqId % 20))) {
 cName = cr.name;
 ward = cr.ward;
 break;
 }
 }
 result.add("Trip #" + tr.tripId + " | " + tr.regNo + " | " + cName + " (Ward " + ward +
") | " +
 tr.dispatchTime + " | [" + tr.status + "]");
 }
 }
 return result;
 }
 public Connection getConnection() throws SQLException {
 return DriverManager.getConnection(URL);
 }
 public List<TripRecord> getMemTrips() {
 return memTrips;
 }
 public List<ConsumerRecord> getMemConsumers() {
 return memConsumers;
 }
 public List<RequestRecord> getMemRequests() {
 return memRequests;
 }
 }
 //
======================================================================
===
 // 4. BYTE STREAM I/O MODULE (Unit III)
 //
======================================================================
===
 public static class BackupRestoreService {
 public static String backupTrips(JalSevaDAO dao, String filename) throws
IOException, SQLException {
 File file = new File(filename);
 int recordCount = 0;
 try (FileOutputStream fos = new FileOutputStream(file)) {
 if (dao.isSqliteAvailable()) {
 try (Connection conn = dao.getConnection();
 Statement st = conn.createStatement();
 ResultSet rs = st.executeQuery("SELECT trip_id, request_id, reg_no,
dispatched_time, status FROM trips")) {
 while (rs.next()) {
 String record = rs.getInt("trip_id") + "|" +
 rs.getInt("request_id") + "|" +
 rs.getString("reg_no") + "|" +
 rs.getString("dispatched_time") + "|" +
 rs.getString("status") + "\n";
 fos.write(record.getBytes());
 recordCount++;
 }
 }
 } else {
 for (JalSevaDAO.TripRecord tr : dao.getMemTrips()) {
 String record = tr.tripId + "|" + tr.reqId + "|" + tr.regNo + "|" + tr.dispatchTime +
"|" + tr.status + "\n";
 fos.write(record.getBytes());
 recordCount++;
 }
 }
 fos.flush();
 }
 return String.format("BACKUP SUCCESS: %d records written to %s (%d bytes).",
recordCount, filename, file.length());
 }
 public static String restoreAndVerifyTrips(JalSevaDAO dao, String filename) throws
IOException, SQLException {
 File file = new File(filename);
 if (!file.exists()) {
 return "Restore failed: " + filename + " does not exist.";
 }
 int restoredCount = 0;
 int totalBytes = (int) file.length();
 byte[] buffer = new byte[totalBytes];
 try (FileInputStream fis = new FileInputStream(file)) {
 int bytesRead = fis.read(buffer);
 String content = new String(buffer, 0, bytesRead);
 String[] rows = content.split("\n");
 for (String row : rows) {
 if (!row.trim().isEmpty()) {
 restoredCount++;
 }
 }
 }
 int dbCount = 0;
 if (dao.isSqliteAvailable()) {
 try (Connection conn = dao.getConnection();
 Statement st = conn.createStatement();
 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM trips")) {
 if (rs.next()) dbCount = rs.getInt(1);
 }
 } else {
 dbCount = dao.getMemTrips().size();
 }
 String matchStatus = (restoredCount == dbCount) ? "All records match database."
: "Count mismatch (" + dbCount + " in DB vs " + restoredCount + " in file).";
 return String.format("RESTORE & VERIFICATION:\nFile: %s\nRecords Restored:
%d\nBytes Read: %d\nVerification: %s",
 filename, restoredCount, totalBytes, matchStatus);
 }
 }
 //
======================================================================
===
 // 5. MULTITHREADING MODULE (Unit III)
 //
======================================================================
===
 public static class TankerTripThread extends Thread {
 private final JalSevaDAO dao;
 private final String regNo;
 private final int tripId;
 private final int reqId;
 private final Object sharedTankerLock;
 public TankerTripThread(JalSevaDAO dao, String regNo, int tripId, int reqId, Object
lock, int priority) {
 super("TankerTripThread-" + regNo);
 this.dao = dao;
 this.regNo = regNo;
 this.tripId = tripId;
 this.reqId = reqId;
 this.sharedTankerLock = lock;
 setPriority(priority);
 }
 @Override
 public void run() {
 System.out.println(getName() + " | priority " + getPriority() + " | state: " + getState());
 try {
 Thread.sleep(4000); // Simulate delivery travel
 String deliveryTime = new SimpleDateFormat("yyyy-MM-dd
HH:mm:ss").format(new Date());
 synchronized (sharedTankerLock) {
 dao.updateTankerStatus(regNo, "FREE");
 dao.updateRequestStatus(reqId, "DELIVERED");
 dao.completeTrip(tripId, deliveryTime);
 System.out.println(getName() + " | update complete, notify() called");
 sharedTankerLock.notifyAll(); // Inter-thread notification
 }
 } catch (InterruptedException e) {
 System.err.println(getName() + " interrupted: " + e.getMessage());
 } catch (SQLException e) {
 System.err.println(getName() + " Database Error: " + e.getMessage());
 }
 System.out.println(getName() + " | finished | state: " + getState());
 }
 }
 public static class DispatchTask implements Runnable {
 private final JalSevaDAO dao;
 private final Object sharedTankerLock;
 private volatile boolean running = true;
 public DispatchTask(JalSevaDAO dao, Object lock) {
 this.dao = dao;
 this.sharedTankerLock = lock;
 }
 public void stopTask() {
 this.running = false;
 }
 @Override
 public void run() {
 System.out.println("DispatchTask | priority " + Thread.currentThread().getPriority()
+ " | state: " + Thread.currentThread().getState());
 while (running) {
 try {
 synchronized (sharedTankerLock) {
 boolean dispatched = attemptDispatch();
 if (!dispatched) {
 sharedTankerLock.wait(5000);
 }
 }
 Thread.sleep(2000);
 } catch (InterruptedException e) {
 break;
 } catch (Exception e) {
 System.err.println("[DispatchTask] " + e.getMessage());
 }
 }
 }
 private boolean attemptDispatch() throws SQLException {
 int reqId = -1;
 String type = "";
 String freeReg = null;
 if (dao.isSqliteAvailable()) {
 try (Connection conn = dao.getConnection()) {
 String queryReq = "SELECT r.request_id, r.litres, c.type, c.ward FROM requests
r " +
 "JOIN consumers c ON r.consumer_id = c.consumer_id " +
 "WHERE r.status = 'PENDING' " +
 "ORDER BY CASE WHEN c.type = 'Hospital' THEN 0 ELSE 1 END,
r.request_id ASC LIMIT 1";
 try (Statement st = conn.createStatement();
 ResultSet rs = st.executeQuery(queryReq)) {
 if (rs.next()) {
 reqId = rs.getInt("request_id");
 type = rs.getString("type");
 }
 }
 if (reqId == -1) return false;
 String queryTanker = "SELECT reg_no FROM tankers WHERE status = 'FREE'
LIMIT 1";
 try (Statement st = conn.createStatement();
 ResultSet rs = st.executeQuery(queryTanker)) {
 if (rs.next()) {
 freeReg = rs.getString("reg_no");
 }
 }
 }
 } else {
 for (JalSevaDAO.RequestRecord rr : dao.getMemRequests()) {
 if ("PENDING".equals(rr.status)) {
 reqId = rr.reqId;
 type = "Hospital"; // default priority
 for (JalSevaDAO.ConsumerRecord cr : dao.getMemConsumers()) {
 if (cr.id == rr.cid) {
 type = cr.type;
 break;
 }
 }
 break;
 }
 }
 if (reqId == -1) return false;
 for (Tanker t : dao.getAllTankers()) {
 if ("FREE".equals(t.getStatus())) {
 freeReg = t.getRegNo();
 break;
 }
 }
 }
 if (freeReg == null) return false;
 String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new
Date());
 dao.updateTankerStatus(freeReg, "ON_TRIP");
 dao.updateRequestStatus(reqId, "DISPATCHED");
 int tripId = dao.insertTrip(reqId, freeReg, time);
 int priority = "Hospital".equalsIgnoreCase(type) ? Thread.MAX_PRIORITY :
Thread.NORM_PRIORITY;
 TankerTripThread tripThread = new TankerTripThread(dao, freeReg, tripId, reqId,
sharedTankerLock, priority);
 tripThread.start();
 return true;
 }
 }
 //
======================================================================
===
 // 6. DESKTOP GUI CONTROL ROOM (AWT & Event Delegation - Unit IV)
 //
======================================================================
===
 public static class ControlRoomGUI extends Frame implements ActionListener,
ItemListener, KeyListener {
 private static final int DAILY_QUOTA_LITRES = 20000;
 private static final int MAX_VALID_WARD = 50;
 private final List<Tanker> fleet = new ArrayList<>();
 private final LinkedList<WaterRequest> pendingRequests = new LinkedList<>();
 private final Vector<String> tripLogs = new Vector<>();
 private final Set<Integer> servedWards = new HashSet<>();
 private final Set<Integer> wardNumbers = new TreeSet<>();
 private final Map<Integer, WaterConsumer> lookup = new HashMap<>();
 private final Hashtable<String, Double> ratesPerConsumerType = new
Hashtable<>();
 private final Object tankerLock = new Object();
 private final JalSevaDAO dao;
 private DispatchTask dispatchTask;
 private Thread dispatcherThread;
 private final CardLayout cardLayout = new CardLayout();
 private final Panel centerDeck = new Panel(cardLayout);
 private TextField txtConsumerId, txtConsumerName, txtWard, txtLitres;
 private Choice choiceConsumerType;
 private Button btnSubmitRequest, btnClearRequest;
 private TextField txtRegNo, txtCapacity, txtDriver;
 private Choice choiceTankerStatus;
 private Button btnAddTanker, btnRefreshTankers;
 private java.awt.List tankerListDisplay;
 private Button btnManualDispatch, btnRefreshTrips;
 private java.awt.List tripListDisplay;
 private Choice choiceBillingType;
 private TextField txtBillingLitres;
 private Label lblInvoiceAmount;
 private Button btnCalculateBill, btnWardReport, btnExportBackup,
btnRestoreBackup;
 private TextArea statusConsole;
 private Label statusLabel;
 public ControlRoomGUI() {
 super("JalSeva: The Smart Municipal Water Tanker Distribution System");
 this.dao = new JalSevaDAO();
 setupCollections();
 buildUI();
 registerListeners();
 launchDispatcher();
 refreshTankerDisplay();
 refreshTripDisplay();
 if (!dao.isSqliteAvailable()) {
 logInfo("Running with In-Memory Storage Engine (SQLite driver not loaded in
classpath). All features active.");
 } else {
 logInfo("Connected to SQLite Database: watertanker.db successfully.");
 }
 setSize(1020, 720);
 setLocationRelativeTo(null);
 setVisible(true);
 }
 private void setupCollections() {
 ratesPerConsumerType.put("Residential", 0.0);
 ratesPerConsumerType.put("Hospital", 0.0);
 ratesPerConsumerType.put("Institutional", 0.05);
 ratesPerConsumerType.put("Commercial", 0.15);
 for (int i = 1; i <= MAX_VALID_WARD; i++) {
 wardNumbers.add(i);
 }
 }
 private void buildUI() {
 setLayout(new BorderLayout(8, 8));
 setBackground(new Color(242, 244, 248));
 Panel navBar = new Panel(new FlowLayout(FlowLayout.LEFT, 12, 10));
 navBar.setBackground(new Color(24, 43, 73));
 Label brand = new Label("JalSeva Dispatch Control Room");
 brand.setForeground(Color.WHITE);
 brand.setFont(new Font("SansSerif", Font.BOLD, 15));
 navBar.add(brand);
 Button navReq = new Button("Requests [Ctrl+R]");
 Button navTankers = new Button("Tankers");
 Button navTrips = new Button("Trips [F6]");
 Button navBilling = new Button("Billing & Backup");
 Button btnDispatch = new Button("Dispatch [F5]");
 btnDispatch.setBackground(new Color(34, 139, 34));
 btnDispatch.setForeground(Color.WHITE);
 navBar.add(navReq);
 navBar.add(navTankers);
 navBar.add(navTrips);
 navBar.add(navBilling);
 navBar.add(btnDispatch);
 add(navBar, BorderLayout.NORTH);
 buildRequestPanel();
 buildTankerPanel();
 buildTripPanel();
 buildBillingPanel();
 add(centerDeck, BorderLayout.CENTER);
 Panel bottomPanel = new Panel(new BorderLayout(5, 5));
 statusLabel = new Label(" Ready. Shortcuts: Ctrl+R (Requests), F5 (Dispatch), F6
(Trips).");
 statusLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
 bottomPanel.add(statusLabel, BorderLayout.NORTH);
 statusConsole = new TextArea(7, 80);
 statusConsole.setEditable(false);
 statusConsole.setFont(new Font("Monospaced", Font.PLAIN, 12));
 bottomPanel.add(statusConsole, BorderLayout.CENTER);
 add(bottomPanel, BorderLayout.SOUTH);
 navReq.addActionListener(e -> cardLayout.show(centerDeck, "Requests"));
 navTankers.addActionListener(e -> { cardLayout.show(centerDeck, "Tankers");
refreshTankerDisplay(); });
 navTrips.addActionListener(e -> { cardLayout.show(centerDeck, "Trips");
refreshTripDisplay(); });
 navBilling.addActionListener(e -> cardLayout.show(centerDeck, "Billing"));
 btnDispatch.addActionListener(e -> triggerManualDispatch());
 }
 private void buildRequestPanel() {
 Panel panel = new Panel(new GridBagLayout());
 GridBagConstraints gbc = new GridBagConstraints();
 gbc.insets = new Insets(8, 8, 8, 8);
 gbc.fill = GridBagConstraints.HORIZONTAL;
 Label title = new Label("Log Water Supply Request");
 title.setFont(new Font("SansSerif", Font.BOLD, 14));
 gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
 panel.add(title, gbc);
 gbc.gridwidth = 1;
 gbc.gridx = 0; gbc.gridy = 1; panel.add(new Label("Consumer ID:"), gbc);
 txtConsumerId = new TextField(18);
 gbc.gridx = 1; gbc.gridy = 1; panel.add(txtConsumerId, gbc);
 gbc.gridx = 0; gbc.gridy = 2; panel.add(new Label("Consumer Name:"), gbc);
 txtConsumerName = new TextField(18);
 gbc.gridx = 1; gbc.gridy = 2; panel.add(txtConsumerName, gbc);
 gbc.gridx = 0; gbc.gridy = 3; panel.add(new Label("Ward (1-50):"), gbc);
 txtWard = new TextField(18);
 gbc.gridx = 1; gbc.gridy = 3; panel.add(txtWard, gbc);
 gbc.gridx = 0; gbc.gridy = 4; panel.add(new Label("Consumer Category:"), gbc);
 choiceConsumerType = new Choice();
 choiceConsumerType.add("Hospital");
 choiceConsumerType.add("Residential");
 choiceConsumerType.add("Commercial");
 choiceConsumerType.add("Institutional");
 gbc.gridx = 1; gbc.gridy = 4; panel.add(choiceConsumerType, gbc);
 gbc.gridx = 0; gbc.gridy = 5; panel.add(new Label("Litres Required:"), gbc);
 txtLitres = new TextField(18);
 gbc.gridx = 1; gbc.gridy = 5; panel.add(txtLitres, gbc);
 Panel btnPanel = new Panel(new FlowLayout(FlowLayout.RIGHT));
 btnClearRequest = new Button("Clear Form");
 btnSubmitRequest = new Button("Log Water Request");
 btnPanel.add(btnClearRequest);
 btnPanel.add(btnSubmitRequest);
 gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
 panel.add(btnPanel, gbc);
 centerDeck.add(panel, "Requests");
 }
 private void buildTankerPanel() {
 Panel panel = new Panel(new BorderLayout(10, 10));
 Panel form = new Panel(new GridLayout(5, 2, 8, 8));
 form.add(new Label("Registration No:"));
 txtRegNo = new TextField("AP16 TT ");
 form.add(txtRegNo);
 form.add(new Label("Capacity (Litres):"));
 txtCapacity = new TextField("12000");
 form.add(txtCapacity);
 form.add(new Label("Driver Name:"));
 txtDriver = new TextField();
 form.add(txtDriver);
 form.add(new Label("Initial Status:"));
 choiceTankerStatus = new Choice();
 choiceTankerStatus.add("FREE");
 choiceTankerStatus.add("ON_TRIP");
 form.add(choiceTankerStatus);
 btnAddTanker = new Button("Register Tanker");
 btnRefreshTankers = new Button("Refresh Fleet");
 form.add(btnAddTanker);
 form.add(btnRefreshTankers);
 panel.add(form, BorderLayout.WEST);
 Panel listP = new Panel(new BorderLayout());
 listP.add(new Label("Registered Municipal Tankers:"), BorderLayout.NORTH);
 tankerListDisplay = new java.awt.List(15);
 listP.add(tankerListDisplay, BorderLayout.CENTER);
 panel.add(listP, BorderLayout.CENTER);
 centerDeck.add(panel, "Tankers");
 }
 private void buildTripPanel() {
 Panel panel = new Panel(new BorderLayout(10, 10));
 Panel controls = new Panel(new FlowLayout(FlowLayout.LEFT));
 btnManualDispatch = new Button("Dispatch Next Request (F5)");
 btnRefreshTrips = new Button("Refresh Trips (F6)");
 controls.add(btnManualDispatch);
 controls.add(btnRefreshTrips);
 panel.add(controls, BorderLayout.NORTH);
 tripListDisplay = new java.awt.List(18);
 panel.add(tripListDisplay, BorderLayout.CENTER);
 centerDeck.add(panel, "Trips");
 }
 private void buildBillingPanel() {
 Panel panel = new Panel(new GridLayout(7, 2, 10, 10));
 panel.add(new Label("Consumer Type:"));
 choiceBillingType = new Choice();
 choiceBillingType.add("Commercial");
 choiceBillingType.add("Institutional");
 choiceBillingType.add("Residential");
 choiceBillingType.add("Hospital");
 panel.add(choiceBillingType);
 panel.add(new Label("Litres Delivered:"));
 txtBillingLitres = new TextField("12000");
 panel.add(txtBillingLitres);
 panel.add(new Label("Calculated Total:"));
 lblInvoiceAmount = new Label("Rs. 0.00");
 lblInvoiceAmount.setFont(new Font("SansSerif", Font.BOLD, 13));
 panel.add(lblInvoiceAmount);
 btnCalculateBill = new Button("Generate Invoice");
 btnWardReport = new Button("Ward Supply Report");
 panel.add(btnCalculateBill);
 panel.add(btnWardReport);
 btnExportBackup = new Button("Backup (trips.dat)");
 btnRestoreBackup = new Button("Restore & Verify (trips.dat)");
 panel.add(btnExportBackup);
 panel.add(btnRestoreBackup);
 centerDeck.add(panel, "Billing");
 }
 private void registerListeners() {
 addWindowListener(new WindowAdapter() {
 @Override
 public void windowClosing(WindowEvent e) {
 if (dispatchTask != null) dispatchTask.stopTask();
 dispose();
 System.exit(0);
 }
 });
 attachKeyRecursive(this);
 btnSubmitRequest.addActionListener(this);
 btnClearRequest.addActionListener(this);
 btnAddTanker.addActionListener(this);
 btnRefreshTankers.addActionListener(this);
 btnManualDispatch.addActionListener(this);
 btnRefreshTrips.addActionListener(this);
 btnCalculateBill.addActionListener(this);
 btnWardReport.addActionListener(this);
 btnExportBackup.addActionListener(this);
 btnRestoreBackup.addActionListener(this);
 choiceConsumerType.addItemListener(this);
 tripListDisplay.addMouseListener(new MouseAdapter() {
 @Override
 public void mouseClicked(MouseEvent e) {
 String selected = tripListDisplay.getSelectedItem();
 if (selected != null) {
 logInfo("Selected Trip Detail: " + selected);
 }
 }
 });
 }
 private void attachKeyRecursive(Component comp) {
 comp.addKeyListener(this);
 if (comp instanceof Container) {
 for (Component child : ((Container) comp).getComponents()) {
 attachKeyRecursive(child);
 }
 }
 }
 private void launchDispatcher() {
 dispatchTask = new DispatchTask(dao, tankerLock);
 dispatcherThread = new Thread(dispatchTask, "BackgroundDispatcherTask");
 dispatcherThread.setPriority(Thread.NORM_PRIORITY);
 dispatcherThread.start();
 }
 public void logRequest(int consumerId) {
 logRequest(consumerId, 12000, "Normal");
 }
 public void logRequest(int consumerId, int litres) {
 logRequest(consumerId, litres, "Normal");
 }
 public void logRequest(int consumerId, int litres, String urgency) {
 pendingRequests.add(new WaterRequest(pendingRequests.size() + 1,
consumerId, litres, urgency, "PENDING"));
 }
 @Override
 public void actionPerformed(ActionEvent e) {
 Object src = e.getSource();
 if (src == btnSubmitRequest) handleRequestSubmission();
 else if (src == btnClearRequest) clearRequestForm();
 else if (src == btnAddTanker) handleAddTanker();
 else if (src == btnRefreshTankers) refreshTankerDisplay();
 else if (src == btnManualDispatch) triggerManualDispatch();
 else if (src == btnRefreshTrips) refreshTripDisplay();
 else if (src == btnCalculateBill) handleBilling();
 else if (src == btnWardReport) generateWardReport();
 else if (src == btnExportBackup) handleBackup();
 else if (src == btnRestoreBackup) handleRestore();
 }
 @Override
 public void itemStateChanged(ItemEvent e) {
 if (e.getSource() == choiceConsumerType) {
 logInfo("Category switched to: " + choiceConsumerType.getSelectedItem());
 }
 }
 @Override
 public void keyPressed(KeyEvent e) {
 if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_R) {
 cardLayout.show(centerDeck, "Requests");
 txtConsumerId.requestFocus();
 logInfo("Shortcut: Switched to Requests Panel (Ctrl+R)");
 } else if (e.getKeyCode() == KeyEvent.VK_F5) {
 triggerManualDispatch();
 } else if (e.getKeyCode() == KeyEvent.VK_F6) {
 cardLayout.show(centerDeck, "Trips");
 refreshTripDisplay();
 logInfo("Shortcut: Switched to Trips Panel (F6)");
 }
 }
 @Override public void keyTyped(KeyEvent e) {}
 @Override public void keyReleased(KeyEvent e) {}
 // Helper to parse numbers safely and strip non-digit characters like 'l' or 'L'
 private int parseSanitizedInt(String input, String fieldName) throws
NumberFormatException {
 if (input == null) throw new NumberFormatException("Empty " + fieldName);
 String cleaned = input.trim().replaceAll("(?i)[^0-9]", "");
 if (cleaned.isEmpty()) {
 throw new NumberFormatException(fieldName + " contains no valid numeric
digits.");
 }
 return Integer.parseInt(cleaned);
 }
 private void handleRequestSubmission() {
 try {
 int cid = parseSanitizedInt(txtConsumerId.getText(), "Consumer ID");
 String name = txtConsumerName.getText().trim();
 int ward = parseSanitizedInt(txtWard.getText(), "Ward");
 int litres = parseSanitizedInt(txtLitres.getText(), "Litres");
 String type = choiceConsumerType.getSelectedItem();
 if (name.isEmpty()) throw new IllegalArgumentException("Consumer name is
required.");
 if (!wardNumbers.contains(ward)) {
 throw new InvalidWardException("InvalidWardException: Ward " + ward + " is
outside municipal territory (1-50).");
 }
 if (litres > DAILY_QUOTA_LITRES) {
 throw new QuotaExceededException("QuotaExceededException: " + litres + "L
exceeds maximum daily quota of " + DAILY_QUOTA_LITRES + "L.");
 }
 WaterConsumer consumer;
 switch (type) {
 case "Hospital":
 consumer = new HospitalConsumer(cid, name, ward, 120);
 break;
 case "Commercial":
 consumer = new CommercialConsumer(cid, name, ward);
 break;
 case "Institutional":
 consumer = new InstitutionalConsumer(cid, name, ward, "Educational");
 break;
 default:
 consumer = new ResidentialConsumer(cid, name, ward);
 break;
 }
 lookup.put(cid, consumer);
 servedWards.add(ward);
 dao.insertConsumer(cid, name, type, ward, "N/A");
 String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new
Date());
 int reqId = dao.insertRequest(cid, litres, time);
 logRequest(cid, litres, type.equals("Hospital") ? "Emergency" : "Normal");
 logInfo("Logged " + consumer.requestNumber(reqId) + " for " + name + " (" + type
+ ", Priority " + consumer.priorityLevel() + ")");
 clearRequestForm();
 synchronized (tankerLock) {
 tankerLock.notifyAll();
 }
 } catch (NumberFormatException nfe) {
 logError("NumberFormatException: Consumer ID, Ward, and Litres must be
integers (e.g. 55 instead of 55l).");
 } catch (InvalidWardException | QuotaExceededException ex) {
 logError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
 } catch (SQLException | IllegalArgumentException ex) {
 logError("Validation Error: " + ex.getMessage());
 }
 }
 private synchronized void triggerManualDispatch() {
 try {
 int reqId = -1;
 int litres = 0;
 String type = "";
 int ward = 0;
 String cName = "";
 if (dao.isSqliteAvailable()) {
 try (Connection conn = dao.getConnection()) {
 String qReq = "SELECT r.request_id, r.consumer_id, r.litres, c.type, c.ward,
c.name FROM requests r " +
 "JOIN consumers c ON r.consumer_id = c.consumer_id WHERE r.status
= 'PENDING' " +
 "ORDER BY CASE WHEN c.type = 'Hospital' THEN 0 ELSE 1 END,
r.request_id ASC LIMIT 1";
 try (Statement st = conn.createStatement(); ResultSet rs =
st.executeQuery(qReq)) {
 if (rs.next()) {
 reqId = rs.getInt("request_id");
 litres = rs.getInt("litres");
 type = rs.getString("type");
 ward = rs.getInt("ward");
 cName = rs.getString("name");
 }
 }
 }
 } else {
 for (JalSevaDAO.RequestRecord rr : dao.getMemRequests()) {
 if ("PENDING".equals(rr.status)) {
 reqId = rr.reqId;
 litres = rr.litres;
 type = "Hospital";
 ward = (rr.cid % 20) + 1;
 cName = "Consumer " + rr.cid;
 for (JalSevaDAO.ConsumerRecord cr : dao.getMemConsumers()) {
 if (cr.id == rr.cid) {
 type = cr.type;
 ward = cr.ward;
 cName = cr.name;
 break;
 }
 }
 break;
 }
 }
 }
 if (reqId == -1) {
 logInfo("No pending requests to dispatch.");
 return;
 }
 String freeReg = null;
 String driver = null;
 for (Tanker t : dao.getAllTankers()) {
 if ("FREE".equalsIgnoreCase(t.getStatus())) {
 freeReg = t.getRegNo();
 driver = t.getDriver();
 break;
 }
 }
 if (freeReg == null) {
 throw new NoTankerAvailableException("NoTankerAvailableException: All
tankers are currently busy on trips.");
 }
 String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new
Date());
 dao.updateTankerStatus(freeReg, "ON_TRIP");
 dao.updateRequestStatus(reqId, "DISPATCHED");
 int tripId = dao.insertTrip(reqId, freeReg, now);
 statusConsole.append("\n----- DISPATCH SLIP -----\n");
 statusConsole.append("Request: WR-" + ward + String.format("%04d", reqId) + "
(" + type + ")\n");
 statusConsole.append("Consumer: " + cName + ", Ward " + ward + "\n");
 statusConsole.append("Litres: " + litres + "\n");
 statusConsole.append("Tanker: " + freeReg + " (Driver: " + driver + ")\n");
 statusConsole.append("Dispatched: " + now + "\n");
 statusConsole.append("Status: On Trip\n");
 statusConsole.append("-------------------------\n");
 Iterator<WaterRequest> it = pendingRequests.iterator();
 while (it.hasNext()) {
 WaterRequest r = it.next();
 if (r.getRequestId() == reqId) {
 it.remove();
 break;
 }
 }
 refreshTankerDisplay();
 refreshTripDisplay();
 } catch (NoTankerAvailableException ntae) {
 logError(ntae.getMessage());
 } catch (SQLException sqle) {
 logError("SQL Error on Dispatch: " + sqle.getMessage());
 }
 }
 private void handleAddTanker() {
 try {
 String reg = txtRegNo.getText().trim();
 int cap = parseSanitizedInt(txtCapacity.getText(), "Tanker Capacity");
 String driver = txtDriver.getText().trim();
 if (reg.isEmpty() || driver.isEmpty()) {
 logError("Tanker Reg and Driver cannot be blank.");
 return;
 }
 dao.insertTanker(reg, cap, driver);
 fleet.add(new Tanker(reg, cap, driver));
 logInfo("Added Tanker: " + reg + " (" + driver + ")");
 refreshTankerDisplay();
 synchronized (tankerLock) {
 tankerLock.notifyAll();
 }
 } catch (NumberFormatException nfe) {
 logError("NumberFormatException: Tanker capacity must be numeric.");
 } catch (SQLException sqle) {
 logError("Database Error: " + sqle.getMessage());
 }
 }
 private void handleBilling() {
 try {
 int litres = parseSanitizedInt(txtBillingLitres.getText(), "Litres Delivered");
 String type = choiceBillingType.getSelectedItem();
 double rate = ratesPerConsumerType.getOrDefault(type, 0.0);
 double total = litres * rate;
 lblInvoiceAmount.setText(String.format("Rs. %.2f (@ Rs. %.2f/L)", total, rate));
 logInfo("Invoice: " + type + " for " + litres + "L = Rs. " + total);
 } catch (NumberFormatException nfe) {
 logError("NumberFormatException: Litres must be numeric.");
 }
 }
 private void generateWardReport() {
 int[] wardSupply = new int[MAX_VALID_WARD + 1];
 int totalSupplied = 0;
 int completedTrips = 0;
 double commercialBilling = 0.0;
 try {
 if (dao.isSqliteAvailable()) {
 try (Connection conn = dao.getConnection();
 Statement st = conn.createStatement();
 ResultSet rs = st.executeQuery("SELECT c.ward, c.type, r.litres FROM trips t
" +
 "JOIN requests r ON t.request_id = r.request_id " +
 "JOIN consumers c ON r.consumer_id = c.consumer_id WHERE t.status
= 'DELIVERED'")) {
 while (rs.next()) {
 int ward = rs.getInt("ward");
 int litres = rs.getInt("litres");
 String type = rs.getString("type");
 if (ward >= 1 && ward <= MAX_VALID_WARD) {
 wardSupply[ward] += litres;
 }
 totalSupplied += litres;
 completedTrips++;
 if ("Commercial".equalsIgnoreCase(type)) {
 commercialBilling += (litres *
ratesPerConsumerType.get("Commercial"));
 }
 }
 }
 } else {
 for (JalSevaDAO.TripRecord tr : dao.getMemTrips()) {
 if ("DELIVERED".equals(tr.status)) {
 int ward = (tr.reqId % 20) + 1;
 int litres = 8000;
 String type = "Commercial";
 for (JalSevaDAO.ConsumerRecord cr : dao.getMemConsumers()) {
 if (cr.id == (101 + (tr.reqId % 20))) {
 ward = cr.ward;
 type = cr.type;
 break;
 }
 }
 for (JalSevaDAO.RequestRecord rr : dao.getMemRequests()) {
 if (rr.reqId == tr.reqId) {
 litres = rr.litres;
 break;
 }
 }
 if (ward >= 1 && ward <= MAX_VALID_WARD) wardSupply[ward] += litres;
 totalSupplied += litres;
 completedTrips++;
 if ("Commercial".equalsIgnoreCase(type)) {
 commercialBilling += (litres *
ratesPerConsumerType.get("Commercial"));
 }
 }
 }
 }
 int highestWard = 1;
 int maxLitres = 0;
 for (int w = 1; w <= MAX_VALID_WARD; w++) {
 if (wardSupply[w] > maxLitres) {
 maxLitres = wardSupply[w];
 highestWard = w;
 }
 }
 statusConsole.append("\n-- WARD SUPPLY REPORT --\n");
 statusConsole.append("Date: " + new SimpleDateFormat("dd-MMyyyy").format(new Date()) + "\n");
 statusConsole.append("Trips Completed: " + completedTrips + "\n");
 statusConsole.append("Total Supplied: " + totalSupplied + " litres\n");
 statusConsole.append("Highest Ward: Ward " + highestWard + " (" + maxLitres +
" litres)\n");
 statusConsole.append("Commercial Billing: Rs. " + commercialBilling + "\n");
 statusConsole.append("Pending Requests: " + pendingRequests.size() + "\n");
 statusConsole.append("------------------------\n");
 logInfo("Ward Supply Report rendered.");
 } catch (SQLException e) {
 logError("Ward report error: " + e.getMessage());
 }
 }
 private void handleBackup() {
 try {
 String msg = BackupRestoreService.backupTrips(dao, "trips.dat");
 logInfo(msg);
 } catch (Exception ex) {
 logError("Backup Error: " + ex.getMessage());
 }
 }
 private void handleRestore() {
 try {
 String msg = BackupRestoreService.restoreAndVerifyTrips(dao, "trips.dat");
 logInfo(msg);
 } catch (Exception ex) {
 logError("Restore Error: " + ex.getMessage());
 }
 }
 private void refreshTankerDisplay() {
 tankerListDisplay.removeAll();
 fleet.clear();
 try {
 List<Tanker> list = dao.getAllTankers();
 for (Tanker t : list) {
 fleet.add(t);
 tankerListDisplay.add(t.toString());
 }
 } catch (SQLException e) {
 logError("Error loading tankers: " + e.getMessage());
 }
 }
 private void refreshTripDisplay() {
 tripListDisplay.removeAll();
 tripLogs.clear();
 try {
 List<String> trips = dao.getTripDisplayList();
 for (String t : trips) {
 tripLogs.add(t);
 tripListDisplay.add(t);
 }
 } catch (SQLException e) {
 logError("Error loading trips: " + e.getMessage());
 }
 }
 private void clearRequestForm() {
 txtConsumerId.setText("");
 txtConsumerName.setText("");
 txtWard.setText("");
 txtLitres.setText("");
 choiceConsumerType.select(0);
 }
 private void logInfo(String msg) {
 statusLabel.setText(" STATUS: " + msg);
 statusConsole.append("[INFO] " + msg + "\n");
 }
 private void logError(String err) {
 statusLabel.setText(" ERROR: " + err);
 statusConsole.append("[ERROR] " + err + "\n");
 }
 }
 //
======================================================================
===
 // 7. UNCAUGHT EXCEPTION DEMONSTRATION METHOD (Module 4)
 //
======================================================================
===
 public static void runUncaughtExceptionDemo() throws QuotaExceededException {
 int requestedLitres = 35000;
 int maxAllowedQuota = 20000;
 if (requestedLitres > maxAllowedQuota) {
 throw new QuotaExceededException("CRITICAL: Request of " + requestedLitres +
 "L exceeds maximum municipal quota of " + maxAllowedQuota + "L.");
 }
 }
 //
======================================================================
===
 // 8. MAIN ENTRY POINT
 //
======================================================================
===
 public static void main(String[] args) {
 if (args.length > 0 && args[0].equalsIgnoreCase("--uncaught-demo")) {
 try {
 runUncaughtExceptionDemo();
 } catch (QuotaExceededException e) {
 throw new RuntimeException("Uncaught wrapper demo", e);
 }
 } else {
 new ControlRoomGUI();
 }
 }
}