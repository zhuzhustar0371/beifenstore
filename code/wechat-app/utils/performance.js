/**
 * 小程序性能优化工具集
 *
 * diffSetData(page, data, [callback]) — 浅比较 setData，仅传递变更字段
 * patchSetData(page, pathMap)          — 路径式 setData，减少序列化开销
 */

/**
 * 浅比较 setData：仅传递实际变更的字段，减少跨线程传输量。
 *
 * 用法：
 *   const perf = require('../../utils/performance.js');
 *   perf.diffSetData(this, { a: 1, b: 2 });
 *
 * @param {Object} pageInstance Page/Component 实例 (this)
 * @param {Object} nextData     要更新的数据，key-value 形式
 * @param {Function} [callback] setData 完成后的回调
 * @returns {Promise<void>}
 */
function diffSetData(pageInstance, nextData, callback) {
  if (!nextData || typeof nextData !== 'object') {
    return Promise.resolve();
  }

  const currentData = pageInstance.data || {};
  const patch = {};
  let hasChange = false;

  const keys = Object.keys(nextData);
  for (let i = 0; i < keys.length; i++) {
    const key = keys[i];
    const nextValue = nextData[key];
    if (nextValue !== currentData[key]) {
      patch[key] = nextValue;
      hasChange = true;
    }
  }

  if (!hasChange) {
    if (typeof callback === 'function') callback();
    return Promise.resolve();
  }

  return new Promise((resolve) => {
    pageInstance.setData(patch, () => {
      if (typeof callback === 'function') callback();
      resolve();
    });
  });
}

/**
 * 路径式 setData：支持 'a.b.c' 嵌套路径，精确更新深层字段。
 *
 * 用法：
 *   perf.patchSetData(this, {
 *     'stats.balance': '12.00',
 *     'userInfo.nickname': '新昵称'
 *   });
 *
 * @param {Object} pageInstance
 * @param {Object} pathMap  key 为数据路径字符串，value 为要设置的值
 * @param {Function} [callback]
 * @returns {Promise<void>}
 */
function patchSetData(pageInstance, pathMap, callback) {
  if (!pathMap || typeof pathMap !== 'object') {
    return Promise.resolve();
  }

  const keys = Object.keys(pathMap);
  if (keys.length === 0) {
    if (typeof callback === 'function') callback();
    return Promise.resolve();
  }

  return new Promise((resolve) => {
    pageInstance.setData(pathMap, () => {
      if (typeof callback === 'function') callback();
      resolve();
    });
  });
}

module.exports = {
  diffSetData,
  patchSetData
};
