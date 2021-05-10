describe('Installed', () => {
    it('should be installed', async () => {
        expect(await driver.isAppInstalled("tv.twitch.bttvandroid.app")).toEqual(true);
    });
});
