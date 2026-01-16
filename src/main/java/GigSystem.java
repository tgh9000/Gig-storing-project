import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Arrays;
import java.util.Comparator;

import java.time.LocalDateTime;
import java.time.Duration;
import java.sql.Timestamp;
import java.util.Vector;

public class GigSystem {

    public static void main(String[] args) {

        // You should only need to fetch the connection details once
        // You might need to change this to either getSocketConnection() or getPortConnection() - see below
        Connection conn = getSocketConnection();

        boolean repeatMenu = true;

        while(repeatMenu){
            System.out.println("_________________________");
            System.out.println("________GigSystem________");
            System.out.println("_________________________");

            
            System.out.println("q: Quit");

            String menuChoice = readEntry("Please choose an option: ");

            if(menuChoice.length() == 0){
                //Nothing was typed (user just pressed enter) so start the loop again
                continue;
            }
            char option = menuChoice.charAt(0);

            /**
             * If you are going to implement a menu, you must read input before you call the actual methods
             * Do not read input from any of the actual task methods
             */
            switch(option){
                case '1':
                    String gigTemp = readEntry("please enter a gig id ");
                    task1(conn, Integer.parseInt(gigTemp));

                    break;
                case '2':
                    break;
                case '3':
                    break;
                case '4':
                    break;
                case '5':
                    break;
                case '6':
                    break;
                case '7':
                    break;
                case '8':
                    break;
                case 'q':
                    repeatMenu = false;
                    break;
                default: 
                    System.out.println("Invalid option");
            }
        }
    }

    /*
     * You should not change the names, input parameters or return types of any of the predefined methods in GigSystem.java
     * You may add extra methods if you wish (and you may overload the existing methods - as long as the original version is implemented)
    */

    public static String[][] task1(Connection conn, int gigID){
        String[][] returning  = null;
        try
        {
            String sql = "SELECT\r\n" + //
                        "a.actname,\r\n" + //
                        "date_trunc('minute', ag.ontime)::time AS ontime,\r\n" + //
                        "date_trunc('minute', ag.endtime)::time AS endtime\r\n" + //
                        "FROM act a\r\n" + //
                        "JOIN act_gig_view ag ON a.actID = ag.actID\r\n" + //
                        "WHERE ag.gigID = ?\r\n" + //
                        "ORDER BY ag.ontime;";

            
            PreparedStatement stmt = conn.prepareStatement
            (
                sql, 
                ResultSet.TYPE_SCROLL_SENSITIVE,   
                ResultSet.CONCUR_READ_ONLY         
            );
            stmt.setInt(1, gigID); 

            //  Execute the query
            ResultSet rs = stmt.executeQuery();

            rs.last();//move to last element in the result set
            int count = rs.getRow();
            if(count == 0)
            {
                System.out.println("its empty");
            }
            returning = new String[count][3];
            rs.beforeFirst();
            int i = 0;
            while (rs.next()) 
            {
                returning[i][0] = rs.getString("actname");
                returning[i][1] = rs.getString("ontime").substring(0,5);
                returning[i][2] = rs.getString("endtime").substring(0,5);
                i++;
            }
            rs.close();
            stmt.close();

            
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        //System.out.println("size of the returning array " + returning.length);
        return returning;
    }

    public static void task2(Connection conn, String venue, String gigTitle, LocalDateTime gigStart, int adultTicketPrice, ActPerformanceDetails[] actDetails)
    {
        
        try
        {
            conn.setAutoCommit(false);
            String sql = "SELECT\r\n" + //
                        "    venueID\r\n" + //
                        "from venue\r\n" + //
                        "WHERE venueName = ?;";

           
            PreparedStatement stmt = conn.prepareStatement
            (
                sql, 
                ResultSet.TYPE_SCROLL_SENSITIVE,   
                ResultSet.CONCUR_READ_ONLY       
            );
            stmt.setString(1, venue); 

            
            ResultSet rs = stmt.executeQuery();
            int venueID = -1;
            if (rs.next()) 
            {
                venueID = rs.getInt("venueID");
            }
            else
            {
                throw new SQLException("Venue not found");
            }


            //inserting the new gig
            sql  = "INSERT INTO gig (venueID, gigtitle, gigdatetime, gigstatus)\r\n" + //
                    "VALUES (?, ?, ?, ?) RETURNING gigID;";
            stmt = conn.prepareStatement
            (
                sql, 
                ResultSet.TYPE_SCROLL_SENSITIVE,   // Scrollable and sensitive
                ResultSet.CONCUR_READ_ONLY         // Read-only (no updates to the result set)
            );
            stmt.setInt(1, venueID);
            stmt.setString(2, gigTitle);
            stmt.setTimestamp(3, Timestamp.valueOf(gigStart));
            stmt.setString(4, "G");
            rs = stmt.executeQuery();
            int gigID = -1;
            if (rs.next())
            {
                gigID = rs.getInt("gigID");
            }
            else
            {
                throw new SQLException("Gig insertion failed");
            }
            //conn.setAutoCommit(false);// all changes are temp
            Arrays.sort(actDetails,Comparator.comparing(ActPerformanceDetails::getOnTime));
            for(int i = 0 ; i < actDetails.length ; i++)
            {
                sql = "INSERT INTO act_gig (actID, gigID, actgigfee, ontime, duration)\r\n" + //
                        "VALUES (?, ?, ?, ?, ?);";
                stmt = conn.prepareStatement
                (
                    sql, 
                    ResultSet.TYPE_SCROLL_SENSITIVE,   // Scrollable and sensitive
                    ResultSet.CONCUR_READ_ONLY         // Read-only (no updates to the result set)
                );
                stmt.setInt(1, actDetails[i].getActID());
                stmt.setInt(2, gigID);
                stmt.setInt(3, actDetails[i].getFee());
                stmt.setTimestamp(4,Timestamp.valueOf(actDetails[i].getOnTime()));
                stmt.setInt(5, actDetails[i].getDuration());
                
                stmt.executeUpdate();
            }
            if (! checkRule10(conn, gigID) )
            {
                throw new SQLException("Rule 10 violation: Gig must last at least 60 minutes");
                
            }
            if (! checkRule6(conn, gigID) )
            {
                throw new SQLException("Rule 6 violation: There must be at least a 3-hour gap between gigs at the same venue on the same day");
                
            }
            sql = "INSERT INTO gig_ticket (gigID, pricetype, price )"+
                "VALUES (?, 'A', ?)";
            
            stmt = conn.prepareStatement
            (
                sql, 
                ResultSet.TYPE_SCROLL_SENSITIVE,   // Scrollable and sensitive
                ResultSet.CONCUR_READ_ONLY         // Read-only (no updates to the result set)
            );
            stmt.setInt(1, gigID);
            stmt.setInt(2, adultTicketPrice);
            stmt.executeUpdate();
            conn.commit();// make all changes permanent
            rs.close();
            stmt.close();

            
        }
        catch (SQLException e)
        {
            try 
            {
                conn.rollback();
            } 
            catch (SQLException rollbackEx) 
            {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();

        }
        finally
        {
            try 
            {
                conn.setAutoCommit(true);
            } 
            catch (SQLException autoCommitEx) 
            {
                autoCommitEx.printStackTrace();
            }
        }
    }

    public static boolean checkRule10(Connection conn, int gigID) throws SQLException
    {
        LocalDateTime gigStart;
        LocalDateTime latestEnd;

        /* ---- Get gig start time ---- */
        String sql = 
            "SELECT gigdatetime "+
            "FROM gig "+
            "WHERE gigID = ? "+
            "AND gigstatus = 'G' ";

        try 
        {
            PreparedStatement stmt = conn.prepareStatement
            (
                sql, 
                ResultSet.TYPE_SCROLL_SENSITIVE,   // Scrollable and sensitive
                ResultSet.CONCUR_READ_ONLY         // Read-only (no updates to the result set)
            );
            stmt.setInt(1, gigID);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
            {
                return false; // gig does not exist or not active
            }
            gigStart = rs.getTimestamp("gigdatetime").toLocalDateTime();
        
        }
        catch (SQLException e)
        {
            return false;
        }

        /* ---- Get final act end time ---- */
        sql = 
            "SELECT MAX(ontime + INTERVAL '1 minute' * duration) AS latest_end "+
            "FROM act_gig "+
            "WHERE gigID = ? ";

        try 
        {
            PreparedStatement stmt = conn.prepareStatement
            (
                sql, 
                ResultSet.TYPE_SCROLL_SENSITIVE,   // Scrollable and sensitive
                ResultSet.CONCUR_READ_ONLY         // Read-only (no updates to the result set)
            );
            stmt.setInt(1, gigID);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
            {
                return false; // gig does not exist or not active
            }
            latestEnd = rs.getTimestamp("latest_end").toLocalDateTime();
            
        }
        catch (SQLException e)
        {
            return false;
        }
        System.out.println("gig start " + gigStart.toString());
        System.out.println("latest end " + latestEnd.toString());
        /* ---- Rule 10 check ---- */
        return !latestEnd.isBefore(gigStart.plusMinutes(60));
    }

    public static boolean checkRule6(Connection conn, int gigID) throws SQLException
    {
        LocalDateTime gigStart;
        LocalDateTime gigEnd;
        int venueID;

        /* ---- Get this gig's start time and venue ---- */
        String sql = 
            "SELECT gigdatetime, venueID " +
            "FROM gig " +
            "WHERE gigID = ? " +
            "AND gigstatus = 'G' ";

        try (PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setInt(1, gigID);
            try (ResultSet rs = ps.executeQuery())
            {
                if (!rs.next())
                    return false;

                gigStart = rs.getTimestamp("gigdatetime").toLocalDateTime();
                venueID = rs.getInt("venueID");
            }
        }

        /* ---- Get this gig's end time ---- */
        sql = "SELECT MAX(ontime + INTERVAL '1 minute' * duration) AS gig_end " +
            "FROM act_gig " +
            "WHERE gigID = ? ";

        try (PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setInt(1, gigID);
            try (ResultSet rs = ps.executeQuery())
            {
                if (!rs.next() || rs.getTimestamp("gig_end") == null)
                    return false;

                gigEnd = rs.getTimestamp("gig_end").toLocalDateTime();
            }
        }

        /* ---- Check against other gigs at same venue, same day ---- */
        sql = 
            "SELECT " +
            "g.gigdatetime AS other_start, " +
            "MAX(ag.ontime + INTERVAL '1 minute' * ag.duration) AS other_end " +
            "FROM gig g " +
            "JOIN act_gig ag ON ag.gigID = g.gigID " +
            "WHERE g.venueID = ? " +
            "AND g.gigID <> ? " +
            "AND DATE(g.gigdatetime) = DATE(?)" +
            "AND g.gigstatus = 'G' " +
            "GROUP BY g.gigID, g.gigdatetime";

        try (PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setInt(1, venueID);
            ps.setInt(2, gigID);
            ps.setTimestamp(3, Timestamp.valueOf(gigStart));

            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    LocalDateTime otherStart =
                            rs.getTimestamp("other_start").toLocalDateTime();
                    LocalDateTime otherEnd =
                            rs.getTimestamp("other_end").toLocalDateTime();

                    boolean enoughGap =
                            gigStart.isAfter(otherEnd.plusMinutes(180)) ||
                            gigEnd.isBefore(otherStart.minusMinutes(180));

                    if (!enoughGap)
                        return false; // Rule 6 not true
                }
            }
        }
        
        return true; // Rule 6 is correct 
    }


    public static void task3(Connection conn, int gigid, String name, String email, String ticketType)
    {
        try
        {
            conn.setAutoCommit(false); // begin transaction

            /* ---- Check gig exists and is active ---- */
            String sql = "SELECT 1 "+
                "FROM gig " +
                " WHERE gigID = ? " +
                "  AND gigstatus = 'G' ";

            try (PreparedStatement ps = conn.prepareStatement(sql))
            {
                ps.setInt(1, gigid);
                try (ResultSet rs = ps.executeQuery())
                {
                    if (!rs.next())
                        throw new SQLException("Gig does not exist or is not active");
                }
            }

            /* ---- Check ticket type exists for this gig ---- */
            sql = "SELECT price " +
                "FROM gig_ticket " +
                "WHERE gigID = ? " +
                "  AND pricetype = ? ";
            try (PreparedStatement ps = conn.prepareStatement(sql))
            {
                ps.setInt(1, gigid);
                ps.setString(2, ticketType);
                try (ResultSet rs = ps.executeQuery())
                {
                    if (!rs.next())
                    {
                        throw new SQLException("Invalid ticket type for this gig");
                    }
                }
            }

            /* ---- Insert ticket ---- */
            sql = "INSERT INTO ticket (gigID, customerName, customerEmail, pricetype) " +
                "VALUES (?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql))
            {
                ps.setInt(1, gigid);
                ps.setString(2, name);
                ps.setString(3, email);
                ps.setString(4, ticketType);
                ps.executeUpdate();
            }

            conn.commit(); // success
        }
        catch (SQLException e)
        {
            try
            {
                conn.rollback(); // restore DB state to before task
            }
            catch (SQLException rollbackEx)
            {
                rollbackEx.printStackTrace();
            }
        }
        finally
        {
            try
            {
                conn.setAutoCommit(true);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }
////////////////////////
   
    public static String[][] task4(Connection conn, int gigID, String actName)
    {
        String sql = "SELECT a.actID, ag.ontime, ag.duration " + 
            "FROM act a " +
            "JOIN act_gig ag ON a.actID = ag.actID " +
            "WHERE ag.gigID = ? " +
            "AND a.actname = ?";

        int actID;
        LocalDateTime actStart;
        int actDuration;

        try 
        {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, gigID);
            stmt.setString(2, actName);

            try (ResultSet rs = stmt.executeQuery())
            {
                if (!rs.next())
                {
                    return null;
                }

                actID = rs.getInt("actID");
                actStart = rs.getTimestamp("ontime").toLocalDateTime();
                actDuration = rs.getInt("duration");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return null;
        }

        try 
        {
            conn.setAutoCommit(false);
            
            
            sql = "DELETE FROM act_gig WHERE gigID = ? AND actID = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, gigID);
            ps.setInt(2, actID);
            ps.executeUpdate();

            
            sql = "SELECT ontime FROM act_gig " +
                "WHERE gigID = ? AND ontime > ? " +
                "ORDER BY ontime LIMIT 1";
            
            int totalRemoval = actDuration;
            ps = conn.prepareStatement(sql);
            ps.setInt(1, gigID);
            ps.setTimestamp(2, Timestamp.valueOf(actStart));
            ResultSet nextActRs = ps.executeQuery();
            
            if (nextActRs.next()) {
                LocalDateTime nextActTime = nextActRs.getTimestamp("ontime").toLocalDateTime();
                LocalDateTime actEnd = actStart.plusMinutes(actDuration);
                long intervalMinutes = java.time.Duration.between(actEnd, nextActTime).toMinutes();
                totalRemoval = actDuration + (int)intervalMinutes;
            }
            
            
            ps = conn.prepareStatement("SET session_replication_role = replica");
            ps.execute();
            
            
            sql = "UPDATE act_gig " +
                "SET ontime = ontime - INTERVAL '1 minute' * ? " +
                "WHERE gigID = ? " +
                "  AND ontime > ?";

            ps = conn.prepareStatement(sql);
            ps.setInt(1, totalRemoval);
            ps.setInt(2, gigID);
            ps.setTimestamp(3, Timestamp.valueOf(actStart));
            ps.executeUpdate();
            
            
            ps = conn.prepareStatement("SET session_replication_role = DEFAULT");
            ps.execute();

            
            if (!checkIntervalsValid(conn, gigID))
            {
                conn.rollback();
                conn.setAutoCommit(true);
                return cancelEntireGig(conn, gigID);
            }

            
            conn.commit();
            conn.setAutoCommit(true);
            
            
            return task1(conn, gigID);
            
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            
            try 
            {
                conn.rollback();
                conn.setAutoCommit(true);
            } 
            catch (SQLException rollbackEx) 
            {
                rollbackEx.printStackTrace();
            }
            
            return null;
        }
    }

    
    private static boolean checkIntervalsValid(Connection conn, int gigID) throws SQLException
    {
        
        String countSql = "SELECT COUNT(*) as cnt FROM act_gig WHERE gigID = ?";
        PreparedStatement ps = conn.prepareStatement(countSql);
        ps.setInt(1, gigID);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next() && rs.getInt("cnt") <= 1) {
            
            return true;
        }
        
        String sql = 
            "WITH act_times AS (" +
            "    SELECT ontime, " +
            "           ontime + INTERVAL '1 minute' * duration AS endtime " +
            "    FROM act_gig " +
            "    WHERE gigID = ? " +
            "    ORDER BY ontime" +
            "), " +
            "intervals AS (" +
            "    SELECT " +
            "        EXTRACT(EPOCH FROM (LEAD(ontime) OVER (ORDER BY ontime) - endtime))/60 AS gap_minutes " +
            "    FROM act_times" +
            ") " +
            "SELECT COUNT(*) as invalid_count " +
            "FROM intervals " +
            "WHERE gap_minutes IS NOT NULL " +
            "  AND (gap_minutes < 10 OR gap_minutes > 30)";
        
        ps = conn.prepareStatement(sql);
        ps.setInt(1, gigID);
        rs = ps.executeQuery();
        
        if (rs.next()) {
            return rs.getInt("invalid_count") == 0;
        }
        
        return true;
    }

    
    private static String[][] cancelEntireGig(Connection conn, int gigID) throws SQLException
    {
        try {
            conn.setAutoCommit(false);
            
            
            String updateGigSql = "UPDATE gig SET gigstatus = 'C' WHERE gigID = ?";
            PreparedStatement ps = conn.prepareStatement(updateGigSql);
            ps.setInt(1, gigID);
            ps.executeUpdate();
            
            
            String updateTicketsSql = "UPDATE ticket SET cost = 0 WHERE gigID = ?";
            ps = conn.prepareStatement(updateTicketsSql);
            ps.setInt(1, gigID);
            ps.executeUpdate();
            
            
            String getCustomersSql = 
                "SELECT DISTINCT customername, customeremail " +
                "FROM ticket " +
                "WHERE gigID = ? " +
                "ORDER BY customername";
            
            ps = conn.prepareStatement(getCustomersSql);
            ps.setInt(1, gigID);
            ResultSet rs = ps.executeQuery();
            
            ArrayList<String[]> customers = new ArrayList<>();
            while (rs.next()) {
                customers.add(new String[] {
                    rs.getString("customername"),
                    rs.getString("customeremail")
                });
            }
            
            conn.commit();
            conn.setAutoCommit(true);
            return customers.toArray(new String[0][0]);
            
        } catch (SQLException e) {
            conn.rollback();
            conn.setAutoCommit(true);
            throw e;
        }
    }
////////////////////////////////////////////////////////////////////////////////////
    public static String[][] task5(Connection conn)
    {
        try
        {
            String sql =
            "SELECT " +
            "    g.gigID, " +
            "    (v.hirecost + COALESCE((SELECT SUM(actgigfee) FROM (SELECT DISTINCT actID, actgigfee FROM act_gig WHERE gigID = g.gigID) AS unique_acts), 0)) AS total_cost, " +
            "    (SELECT MIN(price) FROM gig_ticket WHERE gigID = g.gigID AND price > 0) AS cheapest_price, " +
            "    COALESCE((SELECT SUM(cost) FROM ticket WHERE gigID = g.gigID AND cost > 0), 0) AS revenue " +
            "FROM gig g " +
            "JOIN venue v ON g.venueID = v.venueID " +
            "WHERE g.gigstatus = 'G' " +
            "ORDER BY g.gigID";
            
            ArrayList<String[]> result = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    int gigID = rs.getInt("gigID");
                    
                    
                    long totalCost = rs.getLong("total_cost");
                    Integer cheapestObj = (Integer) rs.getObject("cheapest_price");
                    Long revenueObj = (Long) rs.getObject("revenue");
                    
                    long revenue = (revenueObj != null) ? revenueObj : 0L;
                    int ticketsNeeded = 0;

                    if (cheapestObj != null && cheapestObj > 0) {
                        int cheapest = cheapestObj;
                        long remaining = totalCost - revenue;
                        
                        if (remaining > 0) {
                            
                            ticketsNeeded = (int) Math.ceil((double) remaining / (double) cheapest);
                        }
                    }

                    result.add(new String[] {
                        String.valueOf(gigID),
                        String.valueOf(ticketsNeeded)
                    });
                }
            }

            return result.toArray(new String[0][0]);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }



    





    public static String[][] task6(Connection conn)
    {
        try
        {
            String sql = 
                "WITH headline_acts AS ( " +
                "    SELECT DISTINCT ag.actID, ag.gigID " +
                "    FROM act_gig ag " +
                "    JOIN gig g ON ag.gigID = g.gigID " +
                "    WHERE g.gigstatus = 'G' " +
                "      AND NOT EXISTS ( " +
                "          SELECT 1 " +
                "          FROM act_gig ag2 " +
                "          WHERE ag2.gigID = ag.gigID " +
                "            AND ag2.ontime > ag.ontime " +
                "      ) " +
                "), " +
                "yearly_tickets AS ( " +
                "    SELECT " +
                "        a.actname, " +
                "        EXTRACT(YEAR FROM g.gigdatetime)::INTEGER AS year_num, " +
                "        COUNT(t.ticketID)::INTEGER AS ticket_count " +
                "    FROM headline_acts ha " +
                "    JOIN act a ON ha.actID = a.actID " +
                "    JOIN gig g ON ha.gigID = g.gigID " +
                "    LEFT JOIN ticket t ON g.gigID = t.gigID " +
                "    GROUP BY a.actname, EXTRACT(YEAR FROM g.gigdatetime)::INTEGER " +
                "    HAVING COUNT(t.ticketID) > 0 " +
                "), " +
                "total_tickets AS ( " +
                "    SELECT " +
                "        actname, " +
                "        SUM(ticket_count)::INTEGER AS total_count " +
                "    FROM yearly_tickets " +
                "    GROUP BY actname " +
                "), " +
                "combined_data AS ( " +
                "    SELECT " +
                "        yt.actname, " +
                "        yt.year_num::TEXT AS year, " +
                "        yt.ticket_count::TEXT AS tickets, " +
                "        tt.total_count AS act_total, " +
                "        0 AS is_total, " +
                "        yt.year_num AS sort_year " +
                "    FROM yearly_tickets yt " +
                "    JOIN total_tickets tt ON yt.actname = tt.actname " +
                "    UNION ALL " +
                "    SELECT " +
                "        actname, " +
                "        'Total' AS year, " +
                "        total_count::TEXT AS tickets, " +
                "        total_count AS act_total, " +
                "        1 AS is_total, " +
                "        9999 AS sort_year " +
                "    FROM total_tickets " +
                ") " +
                "SELECT " +
                "    actname, " +
                "    year, " +
                "    tickets " +
                "FROM combined_data " +
                "ORDER BY " +
                "    act_total, " +
                "    actname, " +
                "    is_total, " +
                "    sort_year";
            
            ArrayList<String[]> result = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    result.add(new String[] {
                        rs.getString("actname"),
                        rs.getString("year"),
                        rs.getString("tickets")
                    });
                }
            }

            return result.toArray(new String[0][0]);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static String[][] task7(Connection conn){
        try
        {
            String sql = 
                "WITH headline_acts AS ( " +
                "    SELECT DISTINCT ag.actID, ag.gigID " +
                "    FROM act_gig ag " +
                "    JOIN gig g ON ag.gigID = g.gigID " +
                "    WHERE g.gigstatus = 'G' " +
                "      AND NOT EXISTS ( " +
                "          SELECT 1 " +
                "          FROM act_gig ag2 " +
                "          WHERE ag2.gigID = ag.gigID " +
                "            AND ag2.ontime > ag.ontime " +
                "      ) " +
                "), " +
                "headline_gigs_with_years AS ( " +
                "    SELECT " +
                "        ha.actID, " +
                "        a.actname, " +
                "        ha.gigID, " +
                "        EXTRACT(YEAR FROM g.gigdatetime)::INTEGER AS year " +
                "    FROM headline_acts ha " +
                "    JOIN act a ON ha.actID = a.actID " +
                "    JOIN gig g ON ha.gigID = g.gigID " +
                "), " +
                "act_years AS ( " +
                "    SELECT " +
                "        actname, " +
                "        COUNT(DISTINCT year) AS total_years " +
                "    FROM headline_gigs_with_years " +
                "    GROUP BY actname " +
                "), " +
                "customer_years AS ( " +
                "    SELECT " +
                "        hg.actname, " +
                "        t.customername, " +
                "        COUNT(DISTINCT hg.year) AS years_attended " +
                "    FROM headline_gigs_with_years hg " +
                "    JOIN ticket t ON hg.gigID = t.gigID " +
                "    GROUP BY hg.actname, t.customername " +
                "), " +
                "customer_ticket_counts AS ( " +
                "    SELECT " +
                "        hg.actname, " +
                "        t.customername, " +
                "        COUNT(t.ticketID) AS total_tickets " +
                "    FROM headline_gigs_with_years hg " +
                "    JOIN ticket t ON hg.gigID = t.gigID " +
                "    GROUP BY hg.actname, t.customername " +
                "), " +
                "regular_customers AS ( " +
                "    SELECT " +
                "        cy.actname, " +
                "        cy.customername, " +
                "        ctc.total_tickets " +
                "    FROM customer_years cy " +
                "    JOIN act_years ay ON cy.actname = ay.actname " +
                "    JOIN customer_ticket_counts ctc ON cy.actname = ctc.actname " +
                "        AND cy.customername = ctc.customername " +
                "    WHERE cy.years_attended = ay.total_years " +
                "), " +
                "acts_without_customers AS ( " +
                "    SELECT " +
                "        ay.actname, " +
                "        '[None]' AS customername, " +
                "        0 AS total_tickets " +
                "    FROM act_years ay " +
                "    WHERE NOT EXISTS ( " +
                "        SELECT 1 " +
                "        FROM regular_customers rc " +
                "        WHERE rc.actname = ay.actname " +
                "    ) " +
                "), " +
                "all_results AS ( " +
                "    SELECT actname, customername, total_tickets " +
                "    FROM regular_customers " +
                "    UNION ALL " +
                "    SELECT actname, customername, total_tickets " +
                "    FROM acts_without_customers " +
                ") " +
                "SELECT " +
                "    actname, " +
                "    customername " +
                "FROM all_results " +
                "ORDER BY " +
                "    actname, " +
                "    total_tickets DESC, " +
                "    customername";
            
            ArrayList<String[]> result = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    result.add(new String[] {
                        rs.getString("actname"),
                        rs.getString("customername")
                    });
                }
            }

            return result.toArray(new String[0][0]);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static String[][] task8(Connection conn)
    {
        try 
        {
            // First, get the average ticket price
            String avgPriceQuery = 
                "SELECT ROUND(AVG(cost)) AS avg_price " +
                "FROM ticket t " +
                "JOIN gig g ON t.gigID = g.gigID " +
                "WHERE g.gigstatus = 'G'";
            
            int avgPrice = 0;
            try (PreparedStatement ps = conn.prepareStatement(avgPriceQuery);
                ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    avgPrice = rs.getInt("avg_price");
                }
            }
            
            // If no valid average price, return empty array
            if (avgPrice <= 0) {
                return new String[0][0];
            }
            
            // Find all feasible act-venue combinations
            String sql = 
                "SELECT " +
                "    v.venuename, " +
                "    a.actname, " +
                "    CEIL((v.hirecost + a.standardfee)::NUMERIC / ?::NUMERIC) AS min_tickets " +
                "FROM venue v " +
                "CROSS JOIN act a " +
                "WHERE CEIL((v.hirecost + a.standardfee)::NUMERIC / ?::NUMERIC) <= v.capacity " +
                "ORDER BY " +
                "    v.venuename ASC, " +
                "    min_tickets DESC";
            
            ArrayList<String[]> result = new ArrayList<>();
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, avgPrice);
                ps.setInt(2, avgPrice);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(new String[] {
                            rs.getString("venuename"),
                            rs.getString("actname"),
                            rs.getString("min_tickets")
                        });
                    }
                }
            }
            
            return result.toArray(new String[0][0]);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
    }

    /**
     * Prompts the user for input
     * @param prompt Prompt for user input
     * @return the text the user typed
     */
    private static String readEntry(String prompt) {
        
        try {
            StringBuffer buffer = new StringBuffer();
            System.out.print(prompt);
            System.out.flush();
            int c = System.in.read();
            while(c != '\n' && c != -1) {
                buffer.append((char)c);
                c = System.in.read();
            }
            return buffer.toString().trim();
        } catch (IOException e) {
            return "";
        }

    }
     
    /**
    * Gets the connection to the database using the Postgres driver, connecting via unix sockets
    * @return A JDBC Connection object
    */
    public static Connection getSocketConnection(){
        Properties props = new Properties();
        props.setProperty("socketFactory", "org.newsclub.net.unix.AFUNIXSocketFactory$FactoryArg");
        props.setProperty("socketFactoryArg",System.getenv("HOME") + "/cs258-postgres/postgres/tmp/.s.PGSQL.5432");
        Connection conn;
        try{
          conn = DriverManager.getConnection("jdbc:postgresql://localhost/cwk", props);
          return conn;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the connection to the database using the Postgres driver, connecting via TCP/IP port
     * @return A JDBC Connection object
     */
    public static Connection getPortConnection() {
        
        String user = "postgres";
        String passwrd = "password";
        Connection conn;

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException x) {
            System.out.println("Driver could not be loaded");
        }

        try {
            conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/cwk?user="+ user +"&password=" + passwrd);
            return conn;
        } catch(SQLException e) {
            System.err.format("SQL State: %s\n%s\n", e.getSQLState(), e.getMessage());
            e.printStackTrace();
            System.out.println("Error retrieving connection");
            return null;
        }
    }

    /**
     * Iterates through a ResultSet and converts to a 2D Array of Strings
     * @param rs JDBC ResultSet
     * @return 2D Array of Strings
     */
     public static String[][] convertResultToStrings(ResultSet rs) {
        List<String[]> output = new ArrayList<>();
        String[][] out = null;
        try {
            int columns = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                String[] thisRow = new String[columns];
                for (int i = 0; i < columns; i++) {
                    thisRow[i] = rs.getString(i + 1);
                }
                output.add(thisRow);
            }
            out = new String[output.size()][columns];
            for (int i = 0; i < output.size(); i++) {
                out[i] = output.get(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public static void printTable(String[][] out){
        int numCols = out[0].length;
        int w = 20;
        int widths[] = new int[numCols];
        for(int i = 0; i < numCols; i++){
            widths[i] = w;
        }
        printTable(out,widths);
    }

    public static void printTable(String[][] out, int[] widths){
        for(int i = 0; i < out.length; i++){
            for(int j = 0; j < out[i].length; j++){
                System.out.format("%"+widths[j]+"s",out[i][j]);
                if(j < out[i].length - 1){
                    System.out.print(",");
                }
            }
            System.out.println();
        }
    }

}
