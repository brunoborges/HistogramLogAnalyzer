/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import org.HdrHistogram.HistogramLogAnalyzer.properties.MWPProperties;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.*;

import java.sql.ResultSet;
import java.sql.SQLException;

class DBManager {

    private DBConnect db = null;

    DBManager() {
        try {
            this.db = new DBConnect(((Long) System.currentTimeMillis()).toString());
            create_base_tables();
            this.db.close_db();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void create_base_tables() {
        try {
            String db_create_j_hst = "CREATE TABLE j_hst("+
                    "latencyAxisValue REAL, " +
                    "percentileAxisValue REAL, " +
                    "percentileValue REAL, " +
                    "countAtValue REAL, " +
                    "tag REAL);";
            db.statement.execute(db_create_j_hst);

            String db_create_j_percentile = "CREATE TABLE j_percentile("+
                    "timelineAxisValue REAL," +
                    "latencyAxisValue REAL," +
                    "tag REAL," +
                    "mwp_percentile REAL," +
                    "mwp_windowLength REAL);";
            db.statement.execute(db_create_j_percentile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
     * Timeline related statements
     */
    void insertTimelineObject(TimelineObject to) {
        try {
            String db_insert_j_percentile = "INSERT INTO j_percentile VALUES(\"" +
                    String.valueOf(to.getTimelineAxisValue()) + "\",\"" +
                    String.valueOf(to.getLatencyAxisValue()) + "\",\"" +
                    to.getTag() + "\",\"" +
                    to.getMwpPercentile() + "\",\"" +
                    to.getMwpWindowLength() + "\");";
            db.statement.execute(db_insert_j_percentile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    TimelineIterator listTimelineObjects(boolean multipleTags, String tag,
                                         MWPProperties.MWPEntry mwpEntry, int topLatencies) {
        String queryString = createTimelineQueryString(multipleTags, tag, mwpEntry, topLatencies);
        ResultSet rs = null;
        try {
            rs = db.statement.executeQuery(queryString);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new TimelineIterator(rs);
    }

    private String createTimelineQueryString(boolean multipleTags, String tag,
                                             MWPProperties.MWPEntry mwpEntry, int topLatencies)
    {
        String topLatenciesString =
            (topLatencies > 0) ? " order by latencyAxisValue desc limit " + topLatencies : "";

        if (multipleTags) {
            MWPProperties.MWPEntry defaultMWPEntry = MWPProperties.getDefaultMWPEntry();
            return "select timelineAxisValue,latencyAxisValue from j_percentile" +
                    " where tag='"+ tag + "'" +
                    " and mwp_percentile='" + defaultMWPEntry.getPercentile() + "'" +
                    " and mwp_windowLength='" + defaultMWPEntry.getWindowLength() + "'" +
                    topLatenciesString + ";";
        } else {
            return "select timelineAxisValue,latencyAxisValue from j_percentile" +
                    " where mwp_percentile='" + mwpEntry.getPercentile() + "'" +
                    " and mwp_windowLength='" + mwpEntry.getWindowLength() + "'" +
                    topLatenciesString + ";";
        }
    }

    /*
     * Percentile related statements
     */

    void insertPercentileObject(PercentileObject po) {
        try {
            String db_insert_j_hst = "INSERT INTO j_hst VALUES (\"" +
                    String.valueOf(po.getLatencyAxisValue()) + "\", \"" +
                    String.valueOf(po.getPercentileAxisValue()) + "\", \"" +
                    String.valueOf(po.getPercentileValue()) + "\", \"" +
                    String.valueOf(po.getCountAtValue()) + "\", \"" +
                    po.getTag() + "\")";

            db.statement.execute(db_insert_j_hst);
        } catch (Exception except) {
            except.printStackTrace();
        }
    }

    PercentileIterator listPercentileObjects(String tag, PercentileObject limitObject) {
        String queryString = createPercentileQueryString(tag, limitObject);
        ResultSet rs = null;
        try {
            rs = db.statement.executeQuery(queryString);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new PercentileIterator(rs);
    }

    private String createPercentileQueryString(String tag, PercentileObject limitObject) {
        String  queryString = "select * from j_hst where tag='" + tag + "'";

        if (limitObject != null) {
            queryString += " and latencyAxisValue < " + String.valueOf(limitObject.getLatencyAxisValue());
        }

        queryString += ";";

        return queryString;
    }

    MaxPercentileIterator listMaxPercentileObjects(String tag) {
        String queryString = createMaxValuesQueryString(tag);
        ResultSet rs = null;
        try {
            rs = db.statement.executeQuery(queryString);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new MaxPercentileIterator(rs);
    }

    private String createMaxValuesQueryString(String tag) {
        return "select max( cast(latencyAxisValue as float) ) as MaxLatencyAxisValue, "+
                "max(cast(PercentileAxisValue as float) ) as MaxPercentileAxisValue, "+
                "max(cast(PercentileValue as float) ) as MaxPercentileValue from j_hst"+
                " where tag='"+ tag + "';";
    }

    BucketIterator listBucketObjects(String tag) {
        PercentileIterator pi = listPercentileObjects(tag, null);

        return new BucketIterator(pi);
    }
}
