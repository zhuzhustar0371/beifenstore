const { resolveAssetUrl } = require('./product.js');

function normalizeOrder(order) {
  if (!order || typeof order !== 'object') {
    return null;
  }

  const quantity = Number(order.quantity || 0);
  const productAmountNumber = Number(order.productAmount != null ? order.productAmount : (order.totalAmount || 0));
  const shippingFeeNumber = Number(order.shippingFee || 0);
  const totalAmountNumber = Number(order.totalAmount != null ? order.totalAmount : (productAmountNumber + shippingFeeNumber));
  const cashbackBaseAmountNumber = Number(order.cashbackBaseAmount != null ? order.cashbackBaseAmount : productAmountNumber);
  const productName = typeof order.productName === 'string' && order.productName.trim()
    ? order.productName.trim()
    : (order.productId ? `商品 #${order.productId}` : '商品');
  const productImageUrl = resolveAssetUrl(order.productImageUrl) || '/images/product-placeholder.png';

  return {
    ...order,
    productName,
    productImageUrl,
    productAmount: productAmountNumber.toFixed(2),
    shippingFee: shippingFeeNumber.toFixed(2),
    totalAmount: totalAmountNumber.toFixed(2),
    cashbackBaseAmount: cashbackBaseAmountNumber.toFixed(2),
    unitPrice: quantity > 0 ? (productAmountNumber / quantity).toFixed(2) : '0.00'
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
