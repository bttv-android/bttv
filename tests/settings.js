describe("Settings", () => {
  it("should open BTTV settings", async () => {
    $("#profile_pic_toolbar_image").click();
    $("*=Account Settings").click();
    const el = $("menu_item_title=BTTV");
    expect(el).toBeVisible();
    expect($("AppCompatTextView*=BTTV v")).toExist();
    el.click();

    expect($("#header_text")).toBeVisible();
    expect($("*=BTTV Emotes")).toBeVisible();
    expect($("*=BTTV Gif Emotes")).toBeVisible();
    expect($("*=FrankerFaceZ Emotes")).toBeVisible();
  });
});
