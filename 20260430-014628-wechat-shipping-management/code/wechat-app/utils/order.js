const { resolveAssetUrl } = require('./product.js');

function normalizeOrder(order) {
  if (!order || typeof order !== 'object') {
    return null;
  }

  const quantity = Number(order.quantity || 0);
  const totalAmount = Number(order.totalAmount || 0);
  const productName = typeof order.productName === 'string' && order.productName.trim()
    ? order.productName.trim()
    : (order.productId ? `商品 #${order.productId}` : '商品');
  const productImageUrl = resolveAssetUrl(order.productImageUrl) || '/images/product-placeholder.png';

  return {
    ...order,
    productName,
    productImageUrl,
    unitPrice: quantity > 0 ? (totalAmount / quantity).toFixed(2) : '0.00'
  };
}

function normalizeOrderList(orders) {
  if (!Array.isArray(orders)) {
    return [];
  }

  return orders
    .map(normalizeOrder)
    .filter(Boolean);
}

module.exports = {
  normalizeOrder,
  normalizeOrderList
};
