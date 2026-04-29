const config = require('./config.js');

function resolveAssetOrigin() {
  const baseUrl = (config.baseUrl || '').trim();
  if (!baseUrl) {
    return '';
  }

  return baseUrl.replace(/\/api\/?$/i, '');
}

function resolveAssetUrl(url) {
  if (!url || typeof url !== 'string') {
    return '';
  }

  const trimmed = url.trim();
  if (!trimmed) {
    return '';
  }

  if (/^(https?:)?\/\//i.test(trimmed) || trimmed.indexOf('data:') === 0) {
    return trimmed;
  }

  const origin = resolveAssetOrigin();
  if (!origin) {
    return trimmed;
  }

  return `${origin}${trimmed.charAt(0) === '/' ? trimmed : `/${trimmed}`}`;
}

function normalizeProduct(product) {
  if (!product || typeof product !== 'object') {
    return null;
  }

  const price = Number(product.price || 0);

  return {
    ...product,
    price,
    priceText: formatPrice(price),
    featured: Boolean(product.featured),
    imageUrl: resolveAssetUrl(product.imageUrl)
  };
}

function normalizeProductList(products) {
  if (!Array.isArray(products)) {
    return [];
  }

  return products
    .map(normalizeProduct)
    .filter(Boolean);
}

function formatPrice(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) {
    return '0.00';
  }
  return amount.toFixed(2).replace(/\.00$/, '');
}

function findFeaturedProduct(products) {
  if (!Array.isArray(products) || products.length === 0) {
    return null;
  }
  return products.find((product) => product && product.featured) || products[0];
}

module.exports = {
  findFeaturedProduct,
  formatPrice,
  normalizeProduct,
  normalizeProductList,
  resolveAssetUrl
};
