Component({
  data: {
    selected: 0,
    list: [
      {
        pagePath: '/pages/index/index',
        text: '首页',
        iconPath: '/images/tab-home.png',
        selectedIconPath: '/images/tab-home-active.png'
      },
      {
        pagePath: '/pages/user/user',
        text: '我的',
        iconPath: '/images/tab-user.png',
        selectedIconPath: '/images/tab-user-active.png'
      }
    ]
  },

  methods: {
    switchTab(e) {
      const { index, path } = e.currentTarget.dataset;
      if (typeof index === 'number' && index === this.data.selected) {
        return;
      }

      this.setData({ selected: index });
      wx.switchTab({ url: path });
    }
  }
});
