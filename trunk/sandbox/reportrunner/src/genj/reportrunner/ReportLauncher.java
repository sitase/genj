package genj.reportrunner;

import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.io.GedcomReader;
import genj.report.Report;
import genj.report.ReportLoader;
import genj.util.Origin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs a report based on a set of options.
 *
 * @author Przemek Wiech <pwiech@losthive.org>
 * @version $Id: ReportLauncher.java,v 1.1 2008-11-15 23:27:40 pewu Exp $
 */
public class ReportLauncher
{
    public static final String INDIVIDUAL_OPTION = "individual";
    public static final String FORMAT_OPTION = "format";
    public static final String OUTPUT_OPTION = "output";
    public static final String REPORT_OPTION = "report";
    public static final String GEDCOM_OPTION = "gedcom";

    /**
     * Creates proxies for reports.
     */
    private ReportProxyFactory proxyFactory = new ReportProxyFactory();

    /**
     * Maps report names to report objects.
     */
    private Map<String, Report> reportsByName = new HashMap<String, Report>();

    /**
     * Maps report names to report proxies.
     */
    private Map<String, ReportProxy> proxiesByName = new HashMap<String, ReportProxy>();

    /**
     * Currently open gedcom file.
     */
    private Gedcom gedcom = null;


    /**
     * Currently open gedcom file name.
     */
    private String gedcomFile = null;

    /**
     * Initializes the launcher.
     */
    public ReportLauncher()
    {
        // Load reports
        ReportLoader reportLoader = ReportLoader.getInstance();
        Report[] reports = reportLoader.getReports();
        for (int i = 0; i < reports.length; i++)
            reportsByName.put(reports[i].getClass().getCanonicalName(), reports[i]);
    }

    /**
     * Runs a report based on specified options.
     * @param options  Options for running a report
     */
    public void runReport(Map<String, String> options) throws ReportProxyException, IOException
    {
        ReportRunner.LOG.info("Running report: " + options.get(REPORT_OPTION));
        // get report proxy
        String reportName = options.get(REPORT_OPTION);
        ReportProxy proxy = getProxy(reportName);
        proxy.resetOptions();

        // set report options
        for (Map.Entry<String, String> entry : options.entrySet())
        {
            String key = entry.getKey();
            if (key.equals(OUTPUT_OPTION))
                proxy.setOutputFileName(entry.getValue());
            else if (key.equals(FORMAT_OPTION))
                proxy.setOutputFormat(entry.getValue());
            else if (!key.equals(REPORT_OPTION) && !key.equals(GEDCOM_OPTION) && !key.equals(INDIVIDUAL_OPTION))
                proxy.setOption(key, entry.getValue());
        }

        // if gedcom not loaded, load
        String input = options.get(GEDCOM_OPTION);
        if (input != null && !input.equals(gedcomFile))
        {
            Origin origin = Origin.create(new File(input).toURI().toURL());
            GedcomReader reader = new GedcomReader(origin);
            gedcom = reader.read();
            gedcomFile = input;
        }

        String indiId = options.get(INDIVIDUAL_OPTION);
        Indi indi= (Indi)gedcom.getEntity(Gedcom.INDI, indiId);

        proxy.start(indi);
    }

    /**
     *
     * Gets report proxy from cache or creates one if necessary.
     * @param reportName
     * @return
     */
    private ReportProxy getProxy(String reportName) throws ReportProxyException
    {
        ReportProxy proxy = proxiesByName.get(reportName);
        if (proxy == null)
        {
            proxy = proxyFactory.create(reportsByName.get(reportName));
            proxiesByName.put(reportName, proxy);
        }
        return proxy;
    }

    /**
     * Prints out available reports.
     */
    public void printReports()
    {
        for (Report report : reportsByName.values())
            System.out.println(report.getClass().getCanonicalName() + "\t\t" + report.getName());
    }

    /**
     * Returns available formats for FO-based reports.
     */
    public String getFormats()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (String format : ReportProxy.getFormats())
            sb.append(format).append("|");
        sb.deleteCharAt(sb.length() - 1);
        sb.append(")");
        return sb.toString();
    }

    /**
     * Prints options for a given report.
     * @param reportName  Name of the report
     */
    public void printOptions(String reportName) throws ReportProxyException
    {
        ReportProxy proxy = getProxy(reportName);
        Collection<ReportOption> options = proxy.getOptions();

        System.out.println("These options are supposed to be used in configuration files");
        System.out.println();

        // Print common options
        System.out.println("Common options:");
        System.out.println("   " + REPORT_OPTION + ":" + reportName + "\t\tspecifies this report");
        System.out.println("   " + GEDCOM_OPTION + "\t\tgedcom file to use");
        System.out.println("   " + INDIVIDUAL_OPTION + "\t\tstarting individual");
        System.out.println("   " + OUTPUT_OPTION + "\t\toutput file");
        System.out.println("   " + FORMAT_OPTION + "\t\toutput file format (only FO-based reports)" + getFormats());
        System.out.println();

        // Print report specific options
        System.out.println("Report specific options:");
        for (ReportOption option : options)
        {
            System.out.println("   " + option.getName() + "\t\t" + option.getDescription() + "\t\t" + option.getType() + "  [" + option.getDefaultValue() + "]");
        }
    }
}
