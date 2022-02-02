rm disass -rf
rm old -rf
mv old.apk twitch.apk
rm .bttv-cache -rf
NOBUILD=true ./initworkspace
rm .bttv-cache -rf
mv disass old
cd old
git checkout -b new base
cd ..
mv twitch.apk old.apk
mv new.apk twitch.apk
./disassemble
cp old/.gitignore disass/.
cp old/.git disass/. -r
cd disass
git add .
git commit -m "new"
git tag update
git merge master
git merge --abort

