$('#mysidebar').height($(".nav").height());

// The updateVersionMenu function populates a "package Versions" menu defined in topnav.yml.
// The function is called on $(document).ready() below.
// The 'package' argument is string containing the canonical package name.
// DD 2020-04
function updateVersionMenu(package) {
  // Get list of versions from a static JSON file. Usually this will live on the
  // docs.unidata.ucar.edu server, but during development it's a local file.
  var thisLoc = window.location.origin;
  var jsonFileSuffix = "_doc_versions.json";
  if (thisLoc=="https://docs.unidata.ucar.edu") {
    var versionQuery = thisLoc + '/' + package + '/' + package + jsonFileSuffix;
  } else if (thisLoc=="http://127.0.0.1:4005") {
    var versionQuery = thisLoc + '/files/' + package + jsonFileSuffix;
  } else {
    var versionQuery = '';
  }
  // Get the version currently being displayed from an HTML meta tag.
  var thisVersion = $('meta[name=doc_version]').attr("content");
  // Get the page currently being displayed, so we can go there in the selected doc set.
  var thisPage = window.location.pathname;
  thisPage = thisPage.substring(thisPage.lastIndexOf("/"));

  // Get version info from static JSON file
  var request = $.ajax({
    dataType: "json",
    url: versionQuery,
    method: "GET",
    async: true
  });

  request.success(function(data) {
    // Remove menu placeholder
    $("li#remove").remove();

    // Build menu entries
    $.each(data.releases, function(index, rel) {
      var pVer = rel.version;
      var pDoc = rel.docURL;
      // docURL is a full path to the docset index file. Build path to current page instead.
      pDoc = pDoc.substring(0, pDoc.lastIndexOf("/"));
      pDoc = pDoc + ((typeof thisPage === 'string') ? thisPage : "/");
      // Build the link
      var menuURL = "<li><a href=\""+ pDoc +"\">" + " Version " + pVer + "</a></li>";
      // Add menuURL to menu if json file says it's okay, and if it's not the current version.
      if (parseInt(rel.include) && (thisVersion != rel.version)) {
        $("#docmenu").append(menuURL);
      }
    });
  });

  request.error(function(data) {
    // Remove menu placeholder
    $("li#remove").remove();
    // Insert an error indicator. Maybe this should link somewhere?
    $("#docmenu").append("<li><a href=\"\">Error finding other versions...</a></li>");
    console.log("The JSON file containing version information was not found.");
  });
};
// end updateMenu()

$(document).ready(function() {

  //this script says, if the height of the viewport is greater than 800px, then insert affix class, which makes the nav bar float in a fixed
  // position as your scroll. if you have a lot of nav items, this height may not work for you.
  var h = $(window).height();
  //console.log (h);
  if (h > 800) {
    $("#mysidebar").attr("class", "nav affix");
  }
  // activate tooltips. although this is a bootstrap js function, it must be activated this way in your theme.
  $('[data-toggle="tooltip"]').tooltip({
    placement: 'top'
  });

  /**
   * AnchorJS
   */
  anchors.add('h2,h3,h4,h5');

  // Specify the package name to update the versions menu
  updateVersionMenu("netcdf-java");
});

// needed for nav tabs on pages. See Formatting > Nav tabs for more details.
// script from http://stackoverflow.com/questions/10523433/how-do-i-keep-the-current-tab-active-with-twitter-bootstrap-after-a-page-reload
$(function() {
  var json, tabsState;
  $('a[data-toggle="pill"], a[data-toggle="tab"]').on('shown.bs.tab', function(e) {
    var href, json, parentId, tabsState;

    tabsState = localStorage.getItem("tabs-state");
    json = JSON.parse(tabsState || "{}");
    parentId = $(e.target).parents("ul.nav.nav-pills, ul.nav.nav-tabs").attr("id");
    href = $(e.target).attr('href');
    json[parentId] = href;

    return localStorage.setItem("tabs-state", JSON.stringify(json));
  });

  tabsState = localStorage.getItem("tabs-state");
  json = JSON.parse(tabsState || "{}");

  $.each(json, function(containerId, href) {
    return $("#" + containerId + " a[href=" + href + "]").tab('show');
  });

  $("ul.nav.nav-pills, ul.nav.nav-tabs").each(function() {
    var $this = $(this);
    if (!json[$this.attr("id")]) {
      return $this.find("a[data-toggle=tab]:first, a[data-toggle=pill]:first").tab("show");
    }
  });
});
