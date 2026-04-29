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

  return {
    ...product,
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

module.exports = {
  normalizeProduct,
  normalizeProductList,
  resolveAssetUrl
};
