pushd libs
rm -r *
cp ../../../../libs/z3-5.3.1-x64-glibc-2.39.zip ./z3.zip
unzip z3.zip
mv z3-5.3.1-x64-glibc-2.39 z3
rm z3.zip
popd
