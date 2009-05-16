/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */


import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.gedcom.TagPath;
import genj.report.Report;

/**
 * @author Carsten Muessig <carsten.muessig@gmx.net>
 * @version 1.02
 * @modified by Ekran
 */

public class ReportNames extends Report {

	public boolean reportOutputBirth  = true;
	public boolean reportOutputDeath  = true;
	public boolean reportOutputMarriage  = true;
	public boolean reportFilterName  = false;
	public boolean reportFilterLine  = false;
	public boolean reportDatesOnlyYears  = false;
	public String StrFilter = "";
	public String reportDetailSeparator = ";";
	public boolean reportAlwaysDetailSeparator  = false;

    /**
     * Main for argument Gedcom
     */
    public void start(Gedcom gedcom) {
       Entity[] indis = gedcom.getEntities(Gedcom.INDI,"");
       for(int i=0; i<indis.length; i++) {
          analyzeIndi((Indi)indis[i]);
       }
    }

	private String getDate(String str)
	{
		if (reportDatesOnlyYears == true & str.length()>4)
			str = str.substring(str.length()-4);
		return str;
	}


    /**
     * Main for argument Family
     */
    public void start(Indi indi) {
       analyzeIndi(indi);
    }

    private String trim(Object o) {
        if(o == null)
            return "";
        return o.toString();
    }

    private void analyzeIndi(Indi indi) {
	    if(indi==null)
            return;
		String str = "";

		// Name
		str += indi.getName();

		// Birth
		if (reportOutputBirth) {
			// any data for birth available? IF yes, print birthsymbol
			if((trim(indi.getBirthAsString()).length()>0) || (trim(indi.getProperty(new TagPath("INDI:BIRT:PLAC"))).length()>0) ) {
				str += reportDetailSeparator + " " + OPTIONS.getBirthSymbol();
			}
			else if (reportAlwaysDetailSeparator) {
				str += reportDetailSeparator;
			}

			// if separator always wanted, give also a separator around the birth symbol
			str += (reportAlwaysDetailSeparator ? reportDetailSeparator : " ");


			// get date of birth
			if(trim(indi.getBirthAsString()).length()>0 ) {
				str += trim(getDate(indi.getBirthAsString()));
			}

			// if separator always wanted
			str += (reportAlwaysDetailSeparator ? reportDetailSeparator : " ");

			// get place of birth
			if(trim(indi.getProperty(new TagPath("INDI:BIRT:PLAC"))).length()>0 ) {
				str += trim(indi.getProperty(new TagPath("INDI:BIRT:PLAC")));
			}
		}
		else if (reportAlwaysDetailSeparator) {
			str += reportDetailSeparator + reportDetailSeparator + reportDetailSeparator ;
		}


		// Death
		if (reportOutputDeath) {
			if(indi.getProperty("DEAT")!=null && ( (trim(indi.getDeathAsString()).length()>0) || (trim(indi.getProperty(new TagPath("INDI:DEAT:PLAC"))).length()>0))) {
				str += reportDetailSeparator + " "+ OPTIONS.getDeathSymbol();
			} else if (reportAlwaysDetailSeparator) {
				str += reportDetailSeparator;
			}

			str += (reportAlwaysDetailSeparator ? reportDetailSeparator : " ");

			if(trim(indi.getDeathAsString()).length()>0)
				str += trim(getDate(indi.getDeathAsString()));

			str += (reportAlwaysDetailSeparator ? reportDetailSeparator : " ");

			if (trim(indi.getProperty(new TagPath("INDI:DEAT:PLAC"))).length()>0) {
				str += trim(indi.getProperty(new TagPath("INDI:DEAT:PLAC")));
			}

		}
		else if (reportAlwaysDetailSeparator) {
			str += reportDetailSeparator + reportDetailSeparator + reportDetailSeparator + reportDetailSeparator ;
		}


		// Marriage
		if (reportOutputMarriage)
		{
			Fam[] families = indi.getFamiliesWhereSpouse();
			for(int i=0; i<families.length; i++) {
				//if (reportAlwaysDetailSeparator ||  ((trim(families[i].getMarriageDate()).length()>0) || (trim(families[i].getProperty(new TagPath("FAM:MARR:PLAC"))).length()>0)) || ((indi != (Indi)families[i].getHusband()) & ((Indi)families[i].getHusband() != null) ) || ( (indi != (Indi)families[i].getWife()) & ((Indi)families[i].getWife() != null) ))
				//  str += reportDetailSeparator; // If flag is true or content is available, print separator

				if ( ((trim(families[i].getMarriageDate()).length()>0) || (trim(families[i].getProperty(new TagPath("FAM:MARR:PLAC"))).length()>0)) || ((indi != (Indi)families[i].getHusband()) && ((Indi)families[i].getHusband() != null) ) || ( (indi != (Indi)families[i].getWife()) && ((Indi)families[i].getWife() != null) )) {
					str += reportDetailSeparator + " ("+OPTIONS.getMarriageSymbol();
				}

				if((trim(families[i].getMarriageDate()).length()>0) || (trim(families[i].getProperty(new TagPath("FAM:MARR:PLAC"))).length()>0)) {
					str += (reportAlwaysDetailSeparator ? reportDetailSeparator : " ");
					str += getDate(trim(families[i].getMarriageDate()));
					str += (reportAlwaysDetailSeparator ? reportDetailSeparator : " ");
					str += trim(families[i].getProperty(new TagPath("FAM:MARR:PLAC")));
				} else if (reportAlwaysDetailSeparator) {
					str += reportDetailSeparator + reportDetailSeparator;
				}

				if ( (indi != (Indi)families[i].getHusband()) && ((Indi)families[i].getHusband() != null) ) {
					if(reportAlwaysDetailSeparator || (trim(families[i].getMarriageDate()).length()>0) || (trim(families[i].getProperty(new TagPath("FAM:MARR:PLAC"))).length()>0))
						str += reportDetailSeparator+" ";
					str += families[i].getHusband().getName();
				}

				if ( (indi != (Indi)families[i].getWife()) && ((Indi)families[i].getWife() != null) ) {
					if(reportAlwaysDetailSeparator || (trim(families[i].getMarriageDate()).length()>0) || (trim(families[i].getProperty(new TagPath("FAM:MARR:PLAC"))).length()>0))
						str += reportDetailSeparator+" ";
					str += families[i].getWife().getName();
				}

				if ( ((trim(families[i].getMarriageDate()).length()>0) || (trim(families[i].getProperty(new TagPath("FAM:MARR:PLAC"))).length()>0)) || ((indi != (Indi)families[i].getHusband()) && ((Indi)families[i].getHusband() != null) ) || ( (indi != (Indi)families[i].getWife()) && ((Indi)families[i].getWife() != null) ))
				str += (reportAlwaysDetailSeparator ? reportDetailSeparator : " ") +")";
			}
		}

		// filter for name
		if (reportFilterName) {
			if (indi.getName().toLowerCase().matches(".*"+StrFilter+".*")) {
				println(str);
			}
		}

		// filter in output line
		if (reportFilterLine) {
			if (str.toLowerCase().matches(".*"+StrFilter+".*")) {
				println(str);
			}
		}

		// without filtering
		if ((reportFilterName == false) & (reportFilterLine == false)) {
			println(str);
			}
		}

	} //ReportNames