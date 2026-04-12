pushd libs
rm -r *
cp ../../../../libs/z3-4.13.3-x64-glibc-2.35.zip ./z3.zip
unzip z3.zip
mv z3-4.13.3-x64-glibc-2.35 z3
rm z3.zip
popd
