(function () {
  const isLocalhost =
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1";
  const rootIndexRef = getRootIndexHref();
  const linkHref = isLocalhost
    ? rootIndexRef
    : `${rootIndexRef}../overview-8f24e1/`;
  console.log(`heere: ${linkHref}`);
  const linkTarget = "_self";

  function getRootIndexHref() {
    const root = typeof window.pathToRoot === "string" ? window.pathToRoot : "";
    return `${root}`;
  }

  function updateFavicon() {
    const root = typeof window.pathToRoot === "string" ? window.pathToRoot : "";
    const href = `${root}images/favicon.ico`;
    const icon =
      document.querySelector('link[rel="icon"]') ||
      document.querySelector('link[rel~="icon"]');
    if (icon) {
      icon.href = href;
      return;
    }

    const link = document.createElement("link");
    link.rel = "icon";
    link.type = "image/x-icon";
    link.href = href;
    document.head.appendChild(link);
  }

  function setLink(el) {
    if (!el) return false;
    el.href = linkHref;
    el.target = linkTarget;
    return true;
  }

  function updateHeaderLinks() {
    let updated = false;

    const titleLink = document.querySelector(".library-name--link");
    if (setLink(titleLink)) updated = true;

    const logoLink = document.querySelector(".navigation-logo a");
    if (setLink(logoLink)) updated = true;

    if (updated) return true;

    const titleFallback = document.querySelector(".library-name");
    if (titleFallback && !titleFallback.closest("a")) {
      const link = document.createElement("a");
      setLink(link);
      titleFallback.parentNode.insertBefore(link, titleFallback);
      link.appendChild(titleFallback);
      updated = true;
    }

    const logoImg = document.querySelector(".navigation-logo img");
    if (logoImg && !logoImg.closest("a")) {
      const link = document.createElement("a");
      setLink(link);
      logoImg.parentNode.insertBefore(link, logoImg);
      link.appendChild(logoImg);
      updated = true;
    }

    return updated;
  }

  function interceptClicks() {
    document.addEventListener(
      "click",
      (event) => {
        const target = event.target;
        if (!(target instanceof Element)) return;

        const link =
          target.closest(".library-name--link") ||
          target.closest(".navigation-logo a") ||
          target.closest(".library-name") ||
          target.closest(".navigation-logo");

        if (!link) return;
        event.preventDefault();
        window.location.href = linkHref;
      },
      true
    );
  }

  function init() {
    updateFavicon();
    interceptClicks();
    if (updateHeaderLinks()) return;

    const observer = new MutationObserver(() => {
      if (updateHeaderLinks()) {
        observer.disconnect();
      }
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
