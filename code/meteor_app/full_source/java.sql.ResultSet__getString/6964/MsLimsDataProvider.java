package com.compomics.relims.model.provider.mslims;

import com.compomics.mascotdatfile.util.interfaces.MascotDatfileInf;
import com.compomics.mascotdatfile.util.interfaces.Modification;
import com.compomics.mascotdatfile.util.mascot.ModificationList;
import com.compomics.mslims.db.accessors.Spectrum_file;
import com.compomics.mslims.util.fileio.MascotGenericFile;
import com.compomics.omssa.xsd.UserMod;
import com.compomics.pride_asa_pipeline.data.mapper.AnalyzerDataMapper;
import com.compomics.pride_asa_pipeline.model.AnalyzerData;
import com.compomics.relims.conf.RelimsProperties;
import com.compomics.relims.manager.processmanager.processguard.RelimsException;
import com.compomics.relims.model.UserModConverter;
import com.compomics.relims.model.beans.RelimsProjectBean;
import com.compomics.relims.model.interfaces.DataProvider;
import com.compomics.relims.model.provider.ConnectionProvider;
import com.compomics.relims.manager.progressmanager.Checkpoint;
import com.compomics.relims.manager.progressmanager.ProgressManager;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;

/**
 * This class is a
 */
public class MsLimsDataProvider implements DataProvider {

    private static MsLimsDataProvider ourInstance = new MsLimsDataProvider();
    private static Logger logger = Logger.getLogger(MsLimsDataProvider.class);

    public static MsLimsDataProvider getInstance() {
        return ourInstance;
    }
    private ResultSet lResultSet;
    private PreparedStatement prs;

    public long getNumberOfSpectraForProject(long aProjectID) {
        long lNumberOfSpectra = 0;
        Statement ps = null;
        ResultSet lResultSet = null;

        try {
            String lQuery = "select count(distinct spectrumid) from spectrum as s where s.l_projectid=" + aProjectID;

            logger.debug("QUERY - " + lQuery.replaceAll("\\?", "" + aProjectID));
            ps = ConnectionProvider.getConnection().createStatement();
            ps.execute(lQuery);
            lResultSet = ps.getResultSet();

            lResultSet.next();
            lNumberOfSpectra = lResultSet.getLong(1);

            lResultSet.close();
            ps.close();

            return lNumberOfSpectra;

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            ProgressManager.setState(Checkpoint.FAILED, e);;
            Thread.currentThread().interrupt();
            if (lResultSet != null) {
                try {
                    lResultSet.close();
                } catch (SQLException ex) {
                    lResultSet = null;
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    ps = null;
                }
            }
        }
        return lNumberOfSpectra;
    }

    public Set<AnalyzerData> getInstrumentsForProject(long aProjectID) {
        Set<AnalyzerData> lInstrumentIDSet = new HashSet<AnalyzerData>();
        PreparedStatement ps = null;
        ResultSet lResultSet = null;
        try {
            String lQuery = "select distinct storageclassname from instrument as s,spectrum as sp where s.instrumentID = sp.l_InstrumentID and sp.l_projectid=?";
            ps = ConnectionProvider.getConnection().prepareStatement(lQuery);
            ps.setLong(1, aProjectID);

            lResultSet = ps.executeQuery();
            while (lResultSet.next()) {

                String lStorageClassName = lResultSet.getString(1);
//TODO modify
                if (lStorageClassName == null) {
                    lStorageClassName = "com.compomics.Esquire";
                }

                if (lStorageClassName.indexOf("QTOF") > 0) {
                    // com.compomics.mslims.util.fileio.QTOFSpectrumStorageEngine
                    lInstrumentIDSet.add(AnalyzerDataMapper.getAnalyzerDataByAnalyzerType("tof"));

                } else if (lStorageClassName.indexOf("Ultraflex") > 0) {
                    // com.compomics.mslims.util.fileio.UltraflexSpectrumStorageEngine
                    lInstrumentIDSet.add(AnalyzerDataMapper.getAnalyzerDataByAnalyzerType("tof"));

                } else if (lStorageClassName.indexOf("Esquire") > 0) {
                    // com.compomics.mslims.util.fileio.EsquireSpectrumStorageEngine
                    lInstrumentIDSet.add(AnalyzerDataMapper.getAnalyzerDataByAnalyzerType("iontrap"));

                } else if (lStorageClassName.indexOf("Fourier") > 0) {
                    // com.compomics.mslims.util.fileio.FourierSpectrumStorageEngine
                    lInstrumentIDSet.add(AnalyzerDataMapper.getAnalyzerDataByAnalyzerType("ft"));

                } else {
                    throw new RelimsException(String.format("Failed to map mslims Instrument StorageEngine (%s) to a AnalyzerData instance!!", lStorageClassName));
                }


            }

            lResultSet.close();
            ps.close();

        } catch (Exception e) {
            if (lResultSet != null) {
                try {
                    lResultSet.close();
                } catch (SQLException ex) {
                    lResultSet = null;
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    ps = null;
                }
            }
            logger.error(e.getMessage(), e);
            ProgressManager.setState(Checkpoint.FAILED, e);;
            Thread.currentThread().interrupt();
        }

        return lInstrumentIDSet;
    }

    public Set<String> getProteinAccessionsForProject(long aProjectID) {
        Set<String> lAccessionSet = Sets.newHashSet();

        try {
            String lQuery = "select distinct accession from identification as i, spectrum as s where i.l_spectrumid=s.spectrumid and s.l_projectid=?";
            prs = ConnectionProvider.getConnection().prepareStatement(lQuery);
            prs.setLong(1, aProjectID);

            lResultSet = prs.executeQuery();
            while (lResultSet.next()) {
                String lAccession = lResultSet.getString(1);
                lAccessionSet.add(lAccession);
            }

            lResultSet.close();
            prs.close();

        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            ProgressManager.setState(Checkpoint.FAILED, e);;
            Thread.currentThread().interrupt();
            if (lResultSet != null) {
                try {
                    lResultSet.close();
                } catch (SQLException ex) {
                    lResultSet = null;
                }
            }
            if (prs != null) {
                try {
                    lResultSet.close();
                } catch (SQLException ex) {
                    lResultSet = null;
                }
            }
        }

        return lAccessionSet;
    }

    public long getNumberOfPeptidesForProject(long aProjectID) {
        long lNumberOfPeptides = 0;
        PreparedStatement ps = null;
        ResultSet lResultSet = null;
        try {
            String lQuery = "select count(distinct sequence) from identification as i, spectrum as s where i.l_spectrumid=s.spectrumid and s.l_projectid=?";
            ps = ConnectionProvider.getConnection().prepareStatement(lQuery);
            ps.setLong(1, aProjectID);
            lResultSet = ps.executeQuery();
            lResultSet.next();
            lNumberOfPeptides = lResultSet.getLong(1);

            lResultSet.close();
            ps.close();

            return lNumberOfPeptides;

        } catch (Exception e) {
            if (lResultSet != null) {
                try {
                    lResultSet.close();
                } catch (SQLException ex) {
                    lResultSet = null;
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    ps = null;
                }
            }
            logger.error(e.getMessage(), e);
            ProgressManager.setState(Checkpoint.FAILED, e);;
            Thread.currentThread().interrupt();
        }
        return lNumberOfPeptides;
    }

    public File getSpectraForProject(long aProjectID) throws IOException {
        // The stats and container thereof.

        int total = 0;
        int needed = 0;

        // for debugging purposes
        int lMaxSpectra;
        if (RelimsProperties.hasSpectrumLimit()) {
            lMaxSpectra = RelimsProperties.getSpectrumLimitCount();
        } else {
            lMaxSpectra = Integer.MAX_VALUE;
        }

        File output = new File(RelimsProperties.getWorkSpace(), aProjectID + ".mgf");
        logger.debug("getting all spectra from project " + aProjectID + " in a local file " + output.getCanonicalPath());


        try {
            // Construct the query.NASTY 
            StringBuffer query = new StringBuffer("select distinct(spectrumid), filename from spectrum where l_projectid=");
            query.append(aProjectID);

            PreparedStatement ps = ConnectionProvider.getConnection().prepareStatement(query.toString());
            ResultSet rs = ps.executeQuery();
            HashMap<Integer, String> lSpectrumids = new HashMap<Integer, String>();
            int lSpectrumCounter = 0;
            while (rs.next() && (lSpectrumCounter++ < lMaxSpectra)) {
                int lID = rs.getInt(1);
                String lFilename = rs.getString(2);
                lSpectrumids.put(lID, lFilename);
            }
            ps.close();
            rs.close();


            query = new StringBuffer(Spectrum_file.getBasicSelect());
            String lSpectrumIdJoiner = Joiner.on(",").join(lSpectrumids.keySet());
            query.append(" where l_spectrumid in (" + lSpectrumIdJoiner + ")");
            String queryString = query.toString();

            ps = ConnectionProvider.getConnection().prepareStatement(queryString);
            rs = ps.executeQuery();

            Vector<String> lSpectrumFiles = new Vector<String>();

            int lCounter = 0;
            while (rs.next()) {
                lCounter++;
                Spectrum_file mgf = new Spectrum_file(rs);
                int lID = (int) mgf.getL_spectrumid();
                String lFilename = lSpectrumids.get(lID);

                MascotGenericFile file = new MascotGenericFile(lFilename, new String(mgf.getUnzippedFile()));
                // Note the use of the 'true' flag, which takes care of substituting the original title with the
                // filename!

                lSpectrumFiles.add(file.toString(true) + "\n\n");
                total++;
            }

            if (!output.exists()) {
                output.createNewFile();
            }

            BufferedWriter bos = new BufferedWriter(new FileWriter(output));
            for (String lSpectrum : lSpectrumFiles) {
                bos.write(lSpectrum);
            }
            bos.flush();
            bos.close();

            rs.close();
            ps.close();

        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            ProgressManager.setState(Checkpoint.FAILED, e);;
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            ConnectionProvider.initiate();

//            Thread.currentThread().stop();
            logger.error(e.getMessage(), e);
        }

        return output;
    }

    public long getNumberOfSearchesForProject(long aProjectid) {

        long lNumberOfSearches = 0;
        PreparedStatement ps = null;
        ResultSet lResultSet = null;
        try {
            String lQuery = "select count(distinct title) from identification as i, spectrum as s where i.l_spectrumid=s.spectrumid and s.l_projectid=?";
            ps = ConnectionProvider.getConnection().prepareStatement(lQuery);
            ps.setLong(1, aProjectid);
            lResultSet = ps.executeQuery();
            lResultSet.next();
            lNumberOfSearches = lResultSet.getLong(1);

            lResultSet.close();
            ps.close();

            return lNumberOfSearches;

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            ProgressManager.setState(Checkpoint.FAILED, e);;
            Thread.currentThread().interrupt();
        } finally {
            if (lResultSet != null) {
                try {
                    lResultSet.close();
                } catch (SQLException ex) {
                    lResultSet = null;
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    ps = null;
                }
            }
        }
        return lNumberOfSearches;
    }

    public RelimsProjectBean buildProjectBean(long aProjectid) {

        RelimsProjectBean lRelimsProjectBean = new RelimsProjectBean(aProjectid);

        ArrayList<ModificationList> lModificationLists = Lists.newArrayList();

        DatfileIterator lIterator = new DatfileIterator(ConnectionProvider.getConnection(), aProjectid);
        while (lIterator.hasNext()) {
            MascotDatfileInf lMascotDatfile = lIterator.next();
            lModificationLists.add(lMascotDatfile.getModificationList());
        }

        ModificationList lModificationList = lModificationLists.get(0);
        ArrayList lFixMods = Lists.newArrayList(lModificationList.getFixedModifications());
        ArrayList lVarMods = Lists.newArrayList(lModificationList.getVariableModifications());
        List<Modification> lAllMascotMods = Lists.newArrayList();
        lAllMascotMods.addAll(lFixMods);
        lAllMascotMods.addAll(lVarMods);

        List<UserMod> lUserModList = Lists.newArrayList();
        for (Modification lMod : lAllMascotMods) {
            UserMod lUserMod = UserModConverter.convert(lMod);
            lUserModList.add(lUserMod);
        }

        lRelimsProjectBean.setStandardModificationList(lUserModList);

        // Set precursor and fragment errors
        Set<AnalyzerData> lAnalyzerDataSet = getInstrumentsForProject(aProjectid);

        double lPrecursorError = 0.0;
        double lFragmentError = 0.0;
        String lMassAnalyzer = "";

        for (AnalyzerData lNext : lAnalyzerDataSet) {

            String lNextMassAnalyzer = lNext.getAnalyzerFamily().name();
            if (!lMassAnalyzer.equals("") && !lMassAnalyzer.equals(lNextMassAnalyzer)) {
                throw new RelimsException(
                        String.format("There are multiple Mass Analyzers in this project!!\t\t"
                        + "first:\t%s\tsecond:\t%s", lMassAnalyzer, lNextMassAnalyzer));
            }

            Double lNextPrecursorMassError = lNext.getPrecursorMassError();
            if (lPrecursorError > 0.0 && lNextPrecursorMassError != lPrecursorError) {
                throw new RelimsException("There are multiple Mass Analyzers with different Precursor Mass errors for this project!!");
            }
            lPrecursorError = lNextPrecursorMassError;

            Double lNextFragmentMassError = lNext.getFragmentMassError();
            if (lFragmentError > 0.0 && lFragmentError == lNextFragmentMassError) {
                throw new RelimsException("There are multiple Mass Analyzers with different Fragment Mass errors for this project!!");
            }
            lFragmentError = lNextFragmentMassError;
        }
        //conversion to PPM from da
        lRelimsProjectBean.setPrecursorError(lPrecursorError);
        lRelimsProjectBean.setFragmentError(lFragmentError);

        return lRelimsProjectBean;
    }

    public String toString() {
        return "MsLimsDataProvider";
    }

    @Override
    public boolean isProjectValuable(String experimentID) {
        return true;
    }

    @Override
    public void clearResources() {
        //TODO code for cleanup operations
    }
}
