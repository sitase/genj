Summary: GenealogyJ
Name: GenealogyJ
Version: @version@
Release: 1
License: GPL
Group: Applications
BuildRoot: %{_builddir}
URL: http://genj.sourceforge.net
Source: http://genj.svn.sourceforge.net/viewvc/genj/trunk/
Prefix: /usr/local
Packager: Nils Meier
BuildArchitectures: noarch

%description
GenealogyJ

%undefine __check_files

%prep

%build
pwd

%install

%files
%attr(755,root,root) /usr/local/genj/run.sh
/usr/local/genj
