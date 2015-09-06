#!/usr/bin/perl

use strict;

my $constfile;
my $itr;
my $const;

$constfile =$ARGV[0];
$itr=0;
open(data,$constfile) || die "could not open the file : $constfile";
print "{";
while($const = <data>) {
 chomp($const);
 if($itr != 0) {
	  print ",";
 }
  print "$const";
  $itr++; 
}
 print "}";
close(data);


