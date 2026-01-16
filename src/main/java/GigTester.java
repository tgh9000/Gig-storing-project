import java.util.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import java.util.Random;
import java.util.GregorianCalendar;
import java.util.Arrays;
import java.time.LocalDateTime;
public class GigTester {
    public static void main(String[] args) throws SQLException
    {

        if(args.length > 0){
            if(args[0].equals("reset")){
                //Using the same seednum will generate consistent 'random' data. 
                //Run the script with a different test number to change the data
                //Or leave as -1 to get 'random' data
                int rSeed = -1;
                if(args.length > 1){
                    try {
                        rSeed = Integer.parseInt(args[1]);
                    } catch (Exception e) {
                        System.err.println("Setting data to random seed");
                        rSeed = -1;
                    }
                }
                generateTestDataMain(rSeed);
            }
            if(args[0].equals("test")){
                String warning = "WARNING: These tests have NOT been fully implemented, it is up to you to read them and check the logic\n"
                + "WARNING: Please note that tests 1,5,6 are based on testbig.sql, tests 7 and 8 are based on testsmall.sql";
                System.out.println(warning);
                System.err.println(warning);
                if(args.length > 1){
                    int test = Integer.parseInt(args[1]);
                    switch(test){
                        case 1:
                            System.out.println("Test 1 status: " + testTask1());
                            break;
                        case 2:
                            System.out.println("Test 2 status: " + testTask2());
                            break;
                        case 3:
                            System.out.println("Test 3 status: " + testTask3());
                            break;
                        case 4:
                            System.out.println("Test 4 status: " + testTask4());
                            break;
                        case 5:
                            System.out.println("Test 5 status: " + testTask5());
                            break;
                        case 6:
                            System.out.println("Test 6 status: " + testTask6());
                            break;
                        case 7:
                            System.out.println("Test 7 status: " + testTask7());
                            break;
                        case 8:
                            System.out.println("Test 8 status: " + testTask8());
                            break;
                        case 9:
                            System.out.println("Test 8 status: " + testTask2Invalid());
                            break;
                    }
                }
            }
        }



        
    }


    
    
  


    







    public static boolean testTask1(){
        String[][] out = GigSystem.task1(GigSystem.getSocketConnection(),11);
        String[] gigacts = {"ViewBee 40", "The Where", "The Selecter"};
        String[] ontime = {"18:00", "19:00", "20:25"};
        String[] offtime = {"18:50","20:10", "21:25"};
        try {
            if(out.length != gigacts.length){
                throw new TestFailedException("Length " + out.length,"Length " + gigacts.length);
            }
            if(out[0].length != 3){
                throw new TestFailedException("Columns " + out[0].length, "3");
            }
            for(int i = 0; i < out.length; i++){
                checkValues(out[i][0],gigacts[i]);
                checkValues(out[i][1],ontime[i]);
                checkValues(out[i][2],offtime[i]);
            }            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
    
    public static boolean testTask2()
    {
        LocalDateTime[] onDates = new LocalDateTime[3];
        onDates[0] = LocalDateTime.of(2021,java.time.Month.NOVEMBER,02,20,00);
        onDates[1] = LocalDateTime.of(2021,java.time.Month.NOVEMBER,02,20,40);
        onDates[2] = LocalDateTime.of(2021,java.time.Month.NOVEMBER,02,21,40);
        ActPerformanceDetails[] apd = new ActPerformanceDetails[3];
        apd[0] = new ActPerformanceDetails(3, 20000, onDates[0], 30);
        apd[1] = new ActPerformanceDetails(4, 30000, onDates[1], 40);
        apd[2] = new ActPerformanceDetails(6, 10000, onDates[2], 20);


    
        GigSystem.task2(GigSystem.getSocketConnection(),venues[3],"The November Party",onDates[0],40,apd);
        System.out.println("Should test task2 - you need to implement the test");
        return false;
        //You should put some test code here to read the state of the database after calling task 2 and check it is what you expected
        //You could also call testTask1 here to check the schedule matches what you think it should
    }

    //This method isn't called by anywhere - you can adapt it if you like
    public static boolean testTask2Invalid(){
        LocalDateTime[] onDates = new LocalDateTime[3];
        onDates[0] = LocalDateTime.of(2021,java.time.Month.NOVEMBER,02,20,00);
        onDates[1] = LocalDateTime.of(2021,java.time.Month.NOVEMBER,02,20,35);
        //Nothing should be added, because there is a gap of more than 20 minutes between the second act and the third act
        onDates[2] = LocalDateTime.of(2021,java.time.Month.NOVEMBER,02,22,20);
        ActPerformanceDetails[] apd = new ActPerformanceDetails[3];
        apd[0] = new ActPerformanceDetails(3, 20000, onDates[0], 30);
        apd[1] = new ActPerformanceDetails(4, 30000, onDates[1], 40);
        apd[2] = new ActPerformanceDetails(6, 10000, onDates[2], 20);

        
        GigSystem.task2(GigSystem.getSocketConnection(),venues[3],"The November Party", onDates[0],40,apd);
        System.out.println("Should test task2Invalid - you need to implement the test");
        return false;
        //You should put some test code here to read the state of the database after calling task 2 and check it is what you expected
        //You could also call testTask1 here to check the schedule matches what you think it should
    }

    public static boolean testTask3(){
        int gigid = 24;
        String name = "B Simpson";
        String email = "bsimpson@testemail";
        Connection conn = GigSystem.getSocketConnection();
        try
        {
            /* ---- Count tickets before ---- */
            String sql = "SELECT COUNT(*) AS cnt " +
                "FROM ticket " +
                "WHERE gigID = ? " +
                "AND customerName = ? " +
                "AND customerEmail = ? " +
                "AND pricetype = ?";

            int beforeCount;

            try (PreparedStatement ps = conn.prepareStatement(sql))
            {
                ps.setInt(1, gigid);
                ps.setString(2, name);
                ps.setString(3, email);
                ps.setString(4, "A");

                try (ResultSet rs = ps.executeQuery())
                {
                    rs.next();
                    beforeCount = rs.getInt("cnt");
                }
            }

            /* ---- Call task 3 ---- */
                
            GigSystem.task3(conn, gigid, name, email, "A");


            /* ---- Count tickets after ---- */
            int afterCount;

            try (PreparedStatement ps = conn.prepareStatement(sql))
            {
                ps.setInt(1, gigid);
                ps.setString(2, name);
                ps.setString(3, email);
                ps.setString(4, "A");

                try (ResultSet rs = ps.executeQuery())
                {
                    rs.next();
                    afterCount = rs.getInt("cnt");
                }
            }

            /* ---- Validate insertion ---- */
            if (afterCount != beforeCount + 1)
            {
                System.out.println("Ticket was not inserted correctly");
                return false;
            }

            System.out.println(" testTask3 passed");
            return true;
        }
        catch (SQLException e)
        {
            System.out.println("Error in testTask3");
            e.printStackTrace();
            return false;
        }


        /*GigSystem.task3(GigSystem.getSocketConnection(), gigid, name, email,"A");

        System.out.println("Should test task3 - you need to implement the test");
        return false;
        //You should put some test code here to read the state of the database after calling task 3 and check it is what you expected
        You could also call testTask1 here to check the scshedule matches what you think it should*/
    }

   public static boolean testTask4(){
    Connection conn = GigSystem.getSocketConnection();
    
    try {
        // First, find a valid gig with multiple acts
        String findGigSql = 
            "SELECT ag.gigID, COUNT(*) as act_count " +
            "FROM act_gig ag " +
            "JOIN gig g ON ag.gigID = g.gigID " +
            "WHERE g.gigstatus = 'G' " +
            "GROUP BY ag.gigID " +
            "HAVING COUNT(*) >= 2 " +
            "ORDER BY ag.gigID " +
            "LIMIT 1";
        
        PreparedStatement ps = conn.prepareStatement(findGigSql);
        ResultSet rs = ps.executeQuery();
        
        int cancelGigID;
        if (rs.next()) {
            cancelGigID = rs.getInt("gigID");
            int actCount = rs.getInt("act_count");
            //System.out.println("DEBUG: Testing with gigID " + cancelGigID + " which has " + actCount + " acts");
        } else {
            //System.out.println("DEBUG: No valid gigs found with multiple acts");
            return false;
        }
        
        // Now find an act to remove (preferably not the first or last)
        String findActSql = 
            "SELECT a.actname, ag.ontime " +
            "FROM act a " +
            "JOIN act_gig ag ON a.actID = ag.actID " +
            "WHERE ag.gigID = ? " +
            "ORDER BY ag.ontime " +
            "OFFSET 1 LIMIT 1"; // Get second act
        
        ps = conn.prepareStatement(findActSql);
        ps.setInt(1, cancelGigID);
        rs = ps.executeQuery();
        
        String actName;
        if (rs.next()) {
            actName = rs.getString("actname");
           // System.out.println("DEBUG: Will remove act: '" + actName + "'");
        } else {
            // If no second act, get the first one
            String firstActSql = 
                "SELECT a.actname " +
                "FROM act a " +
                "JOIN act_gig ag ON a.actID = ag.actID " +
                "WHERE ag.gigID = ? " +
                "ORDER BY ag.ontime " +
                "LIMIT 1";
            
            ps = conn.prepareStatement(firstActSql);
            ps.setInt(1, cancelGigID);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                actName = rs.getString("actname");
                //System.out.println("DEBUG: Will remove first act: '" + actName + "'");
            } else {
                //System.out.println("DEBUG: No acts found");
                return false;
            }
        }
        
        // Get the state BEFORE task4 is called
        boolean hadTickets = checkIfGigHasTickets(conn, cancelGigID);
        int actCountBefore = getActCountForGig(conn, cancelGigID);
        
       // System.out.println("DEBUG: Gig " + cancelGigID + " has " + actCountBefore + " acts and " + (hadTickets ? "has" : "no") + " tickets");
        
        // Call task4
        String[][] result = GigSystem.task4(conn, cancelGigID, actName);
        
        if (result == null) {
            System.out.println("FAIL: task4 returned null");
            return false;
        }
        
        // Check if gig was cancelled
        boolean gigCancelled = isGigCancelled(conn, cancelGigID);
        
        if (gigCancelled) {
            // Test cancelled gig scenario
            System.out.println("Gig was cancelled. Testing cancellation...");
            
            // 1. Verify gigstatus = 'C'
            if (!gigCancelled) {
                System.out.println("FAIL: Gig should be cancelled but isn't");
                return false;
            }
            
            // 2. Verify act_gig records still exist (not deleted)
            int actCountAfter = getActCountForGig(conn, cancelGigID);
            if (actCountAfter != actCountBefore) {
                System.out.println("FAIL: Act records were modified (should remain unchanged when cancelling)");
                System.out.println("Before: " + actCountBefore + ", After: " + actCountAfter);
                return false;
            }
            
            // 3. Verify all ticket costs are 0
            if (hadTickets && !allTicketCostsZero(conn, cancelGigID)) {
                System.out.println("FAIL: Not all ticket costs were set to 0");
                return false;
            }
            
            // 4. Verify returned customer list format
            if (hadTickets && result.length == 0) {
                System.out.println("FAIL: Should have returned customers but got empty array");
                return false;
            }
            
            // Check format: should be [name, email] pairs
            for (String[] customer : result) {
                if (customer.length != 2) {
                    System.out.println("FAIL: Customer array should have 2 elements [name, email]");
                    return false;
                }
            }
            
            System.out.println("PASS: Gig cancellation handled correctly");
            System.out.println("Returned " + result.length + " affected customers");
            
        } else {
            // Test act removal scenario (gig continues)
            System.out.println("Gig continues. Testing act removal...");
            
            // 1. Verify act was removed
            if (actStillInGig(conn, cancelGigID, actName)) {
                System.out.println("FAIL: Act was not removed from gig");
                return false;
            }
            
            int actCountAfter = getActCountForGig(conn, cancelGigID);
           // System.out.println("DEBUG: Acts before: " + actCountBefore + ", after: " + actCountAfter);
            
            // 2. Verify lineup was returned
            if (result.length == 0) {
                System.out.println("FAIL: Empty lineup returned");
                return false;
            }
            
            //System.out.println("DEBUG: Lineup has " + result.length + " rows");
            
            // 3. Verify lineup format (should match task1 format)
            for (String[] row : result) {
                if (row.length < 3) { // At least actname, ontime, duration
                    System.out.println("FAIL: Lineup row has wrong format (expected at least 3 columns)");
                    return false;
                }
            }
            
            // 4. Verify timing is valid (no overlaps, proper intervals)
            if (!verifyTimingValid(conn, cancelGigID)) {
                System.out.println("FAIL: Timing validation failed after act removal");
                return false;
            }
            
            System.out.println("PASS: Act removal handled correctly");
        }
        
        return true;
        
    } catch (Exception e) {
        System.out.println("FAIL: Exception occurred");
        e.printStackTrace();
        return false;
    }
}

// Helper method: Check if gig has tickets sold
private static boolean checkIfGigHasTickets(Connection conn, int gigID) throws SQLException {
    String sql = "SELECT COUNT(*) as cnt FROM ticket WHERE gigID = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, gigID);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("cnt") > 0;
        }
    }
    return false;
}

// Helper method: Get act count for a gig
private static int getActCountForGig(Connection conn, int gigID) throws SQLException {
    String sql = "SELECT COUNT(*) as cnt FROM act_gig WHERE gigID = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, gigID);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("cnt");
        }
    }
    return 0;
}

// Helper method: Check if gig is cancelled
private static boolean isGigCancelled(Connection conn, int gigID) throws SQLException {
    String sql = "SELECT gigstatus FROM gig WHERE gigID = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, gigID);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return "C".equals(rs.getString("gigstatus"));
        }
    }
    return false;
}

// Helper method: Verify all tickets for gig have cost = 0
private static boolean allTicketCostsZero(Connection conn, int gigID) throws SQLException {
    String sql = "SELECT COUNT(*) as cnt FROM ticket WHERE gigID = ? AND cost != 0";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, gigID);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("cnt") == 0;
        }
    }
    return false;
}

// Helper method: Check if act is still in gig
private static boolean actStillInGig(Connection conn, int gigID, String actName) throws SQLException {
    String sql = "SELECT COUNT(*) as cnt FROM act_gig ag " +
                 "JOIN act a ON ag.actID = a.actID " +
                 "WHERE ag.gigID = ? AND a.actname = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, gigID);
        ps.setString(2, actName);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt("cnt") > 0;
        }
    }
    return false;
}

// Helper method: Verify timing constraints are still valid
private static boolean verifyTimingValid(Connection conn, int gigID) throws SQLException {
    // Check for overlaps
    String overlapCheck = 
        "SELECT COUNT(*) as cnt FROM act_gig ag1, act_gig ag2 " +
        "WHERE ag1.gigID = ? AND ag2.gigID = ? " +
        "AND ag1.ontime < ag2.ontime " +
        "AND ag1.ontime + INTERVAL '1 minute' * ag1.duration > ag2.ontime";
    
    try (PreparedStatement ps = conn.prepareStatement(overlapCheck)) {
        ps.setInt(1, gigID);
        ps.setInt(2, gigID);
        ResultSet rs = ps.executeQuery();
        if (rs.next() && rs.getInt("cnt") > 0) {
            System.out.println("Timing validation failed: overlapping acts found");
            return false;
        }
    }
    
    // Check intervals between acts (10-30 minutes)
    String intervalCheck = 
        "WITH act_times AS (" +
        "    SELECT ontime, ontime + INTERVAL '1 minute' * duration AS endtime " +
        "    FROM act_gig WHERE gigID = ? ORDER BY ontime" +
        "), " +
        "intervals AS (" +
        "    SELECT " +
        "        EXTRACT(EPOCH FROM (LEAD(ontime) OVER (ORDER BY ontime) - endtime))/60 AS gap_minutes " +
        "    FROM act_times" +
        ") " +
        "SELECT COUNT(*) as cnt FROM intervals " +
        "WHERE gap_minutes IS NOT NULL AND (gap_minutes < 10 OR gap_minutes > 30)";
    
    try (PreparedStatement ps = conn.prepareStatement(intervalCheck)) {
        ps.setInt(1, gigID);
        ResultSet rs = ps.executeQuery();
        if (rs.next() && rs.getInt("cnt") > 0) {
            System.out.println("Timing validation failed: invalid intervals between acts");
            return false;
        }
    }
    
    return true;
}

// Helper method: Compare two customer arrays
private static boolean compareCustomerArrays(String[][] actual, String[][] expected) {
    if (actual == null && expected == null) return true;
    if (actual == null || expected == null) return false;
    if (actual.length != expected.length) return false;
    
    for (int i = 0; i < actual.length; i++) {
        if (actual[i].length != expected[i].length) return false;
        for (int j = 0; j < actual[i].length; j++) {
            if (!actual[i][j].equals(expected[i][j])) return false;
        }
    }
    return true;
}

// Helper method: Print array for debugging
private static void printArray(String[][] array) {
    if (array == null) {
        System.out.println("null");
        return;
    }
    for (String[] row : array) {
        System.out.println(Arrays.toString(row));
    }
}

    public static boolean testTask5(){
        String[][] out = GigSystem.task5(GigSystem.getSocketConnection());
        //This data should work for the main set of test data.
        int numTickets[] = {1600, 2000, 1525, 1225, 1650, 1525, 1300, 1850, 2023, 398, 1873, 1849, 1125, 1949, 1498, 1073, 1900, 399, 749, 1425, 697, 1098, 2875, 825, 1224, 1849, 1149, 1525, 1625, 1548, 850, 300, 524, 775, 1297, 2522, 1274, 1150, 2250, 1223, 1974, 950, 775, 525, 749, 1800, 1900, 973, 298, 2275};
        try{
            for(int i = 1; i < numTickets.length; i++){
                checkValues(out[i-1][0],String.valueOf(i));
                checkValues(out[i-1][1],String.valueOf(numTickets[i-1]));
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean testTask6(){
        String[][] out = GigSystem.task6(GigSystem.getSocketConnection());
        String[] acts = {"QLS","QLS","QLS","ViewBee 40","ViewBee 40","ViewBee 40","Scalar Swift","Scalar Swift","Scalar Swift","Scalar Swift",
        "Join Division","Join Division","Join Division","Join Division","The Selecter","The Selecter","The Selecter","The Where","The Where","The Where",
        "The Where","The Where"};
        String[] years = {"2018","2019","Total","2017","2018","Total","2017","2018","2019","Total","2016","2018","2020","Total","2017","2018","Total","2016","2017","2018","2020","Total"};
        String[] totals = {"2","1","3","3","1","4","3","1","1","5","2","2","3","7","4","4","8","1","3","5","4","13"};
        try{
            if(out.length != acts.length){
                throw new TestFailedException("Length " + out.length,"Length " + acts.length);
            }
            if(out[0].length != 3){
                throw new TestFailedException("Columns " + out[0].length, "3");
            }
            for(int i = 0; i < acts.length; i++){
                checkValues(out[i][0], acts[i]);
                checkValues(out[i][1], years[i]);
                checkValues(out[i][2], totals[i]);
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean testTask7(){
        //In the test data the solution is...
        String[][] out = GigSystem.task7(GigSystem.getSocketConnection());
        String[] acts = {"Join Division","QLS","Scalar Swift","Scalar Swift"};
        String[] customers = {"G Jones","[None]","G Jones", "J Smith"};


        for(int i = 0; i < acts.length; i++){
            System.out.println(acts[i] + " | " + " | " + customers[i]);
        }

        try {
            if(out.length != acts.length){
                throw new TestFailedException("Length " + out.length,"Length " + acts.length);
            }
            if(out[0].length != 2){
                throw new TestFailedException("Columns " + out[0].length, "2");
            }
            for(int i = 0; i < acts.length; i++){
                checkValues(out[i][0],acts[i]);
                checkValues(out[i][1],customers[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean testTask8(){
        //In the test data the solution is...
        String[][] out = GigSystem.task8(GigSystem.getSocketConnection());
        String[] venues = {"Arts Centre Theatre","Big Hall","Big Hall","Cinema","City Hall","Symphony Hall","Symphony Hall","Symphony Hall",
        "Symphony Hall","Symphony Hall","Symphony Hall","Town Hall","Town Hall","Village Green","Village Hall"};
        String[] acts = {"Join Division","The Where","Join Division","Join Division","Join Division","ViewBee 40","Scalar Swift","QLS","The Selecter","The Where",
        "Join Division","The Where","Join Division","Join Division","Join Division"};
        String[] seats = {"150","675","375","175","225","1275","1250","1225","1200","825","525","575","275","100","75"};
        try {
            if(out.length != acts.length){
                throw new TestFailedException("Length " + out.length,"Length " + acts.length);
            }
            if(out[0].length != 3){
                throw new TestFailedException("Columns " + out[0].length, "3");
            }
            for(int i = 0; i < acts.length; i++){
                checkValues(out[i][0],venues[i]);
                checkValues(out[i][1],acts[i]);
                checkValues(out[i][2],seats[i]);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    //Make your own if you like, these are based on local theatres
    static String[] venues = {"Big Hall","Arts Centre Theatre","City Hall","Village Green","Village Hall","Cinema","Symphony Hall","Town Hall"};

    /** 
    * This method generates an SQL file that you can use to populate the database
    * Note we are outputing statements rather than using JDBC
    * Do not confuse this method with the prepared statements you should be writing in the other file
    * In GigSystem.java, you must use JDBC methods rather than generating SQL files like this
    */
    private static void generateTestDataMain(int seednum) throws SQLException{
        Random rn = null;
        if(seednum == -1){
            rn = new Random();
        }else{
            rn = new Random(seednum);
        }        

        Act[] acts = generateActs(rn);

        int numGigs = 50;
        int[] ticketPrice = new int[numGigs];

        for(int i = 0; i < acts.length; i++){
            System.out.format("INSERT INTO ACT (actid, actname, genre, standardfee) VALUES(%d,'%s','Music',%d);\n",i+1,acts[i].actName,acts[i].standardFee);
        }
        
        for(int i = 0; i < venues.length; i++){
            //The venues intentionally have a very low capacity to make it easier to test capacity without having loads of tickets to worry about!
            System.out.format("INSERT INTO VENUE (venueid, venuename, hirecost, capacity) VALUES (%d,'%s',%d,%d);\n",i+1,venues[i],1000*(1+rn.nextInt(20)),1+rn.nextInt(20));
        }

        for(int i = 0; i < numGigs; i++){
            ticketPrice[i] = 10 * (rn.nextInt(10)+1);
            //Although for now we can assume each ticket costs £40
            ticketPrice[i] = 40;
            int year = 2016 + rn.nextInt(5);
            int month = 1 + rn.nextInt(11);
            //Note this isn't evenly distributed, only considering up to 28th of each month
            int day = 1 + rn.nextInt(28);
            int time = 18 + rn.nextInt(3);
            GregorianCalendar gc = new GregorianCalendar();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            gc.set(year,month,day,time,0);
            sdf.setCalendar(gc);
            Date gigStartDate = gc.getTime();
            System.out.format("INSERT INTO GIG (gigid, venueid, gigtitle, gigdatetime, gigstatus) VALUES (%d,%d,'%s','%s','G');\n",(i+1),(1+rn.nextInt(venues.length)),"Test title",sdf.format(gigStartDate));

            int totalDuration = 0;
            boolean enoughActs = false;
            while(totalDuration < 180 && !enoughActs){
                int actID = 1+rn.nextInt(acts.length);
                int duration = 20 + (10 * rn.nextInt(7));
                Date onDate = new Date(gigStartDate.getTime() + totalDuration * 60 * 1000);

                //If you want to test what happens when there is a gap of more than 20 minutes, modify the below line
                int gap = 5 * rn.nextInt(4);
                totalDuration = totalDuration + duration + gap;
                if(rn.nextInt(3) == 0){
                    enoughActs = true;
                }
                System.out.format("INSERT INTO ACT_GIG (actid, gigid, actgigfee, ontime, duration) VALUES(%d,%d,%d,'%s',%d);\n",actID,(i+1),acts[actID - 1].standardFee,sdf.format(onDate),duration);
            }

            System.out.format("INSERT INTO GIG_TICKET (gigid, pricetype, price) VALUES(%d,'A',%d);\n",(i+1),ticketPrice[i]);
        }

        int numCustomers = 10;

        //You can make more imaginitive customer names if you like
        Customer customers[] = new Customer[numCustomers];
        for(int i = 0; i < numCustomers; i++){
            String fname = Character.toString((char)(97+(i%26))) + i;
            String lname = Character.toString((char)(97+(i%26))) + i;
            String name = fname + " " + lname;
            String email = Integer.toString(i) + Character.toString((char)(97+(i%26))) + Character.toString((char)(97+(i%26)));
            customers[i] = new Customer(name,email);
        }
        
        for(int i = 0; i < 40; i++){
            int custID = rn.nextInt(10);
            int gigID = rn.nextInt(50) + 1;
            System.out.format("INSERT INTO TICKET (ticketid,gigid,CustomerName,CustomerEmail,pricetype,cost) VALUES (DEFAULT,%d,'%s','%s','A',%d);\n",gigID,customers[custID].name,customers[custID].email,ticketPrice[gigID-1]);
        }

    }

    public static Act[] generateActs(Random r){
        //Feel free to add/change act names
        String[] acts = {"QLS", "The Where", "Join Division", "The Selecter", "Scalar Swift", "ViewBee 40"};
        Act[] allActs = new Act[acts.length];
        for(int i = 0; i < acts.length; i++){
            allActs[i] = new Act(r,acts[i]);
        }
        return allActs;
    }

    private static void checkValues(String provided, String expected) throws TestFailedException{
        if(!provided.equals(expected)){
            throw new TestFailedException(provided, expected);
        }
    }
}

class Act{
    public int standardFee;
    public String actName;
    Act(Random r, String name){
        standardFee = 1000 * (1+ r.nextInt(40));
        actName = name;
    }
}

class Customer{
    public String name;
    public String email;
    Customer(String name, String email){
        this.name = name;
        this.email = email;
    }   
}