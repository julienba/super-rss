# Debug New website

## Overview

Your goal is to process the URL to find the best way to create a new feeds from it.

## Steps

### Finding the best method

1. The website has an RSS feed

Use `curl` to fetch the website` and search for a RSS links. If you find one this is the best way to create a new feeds.

2. The website has a sitemap

Using `curl` search if the website has a sitemap. It's often part of the robots.txt

3. Creating a feed from the page

Look for links in the website that looks like blog article and can be used for creating a news feeds.


### Making it works with `core/get-feed`

Using clojure-mcp use the medthod find with curl and the URL to create a news feed. If it's working you can stop here.
If it's not working debug the code to identify why.
Once you find why, create a plan to improve the code to support this website.


### Instructions

- Keep in mind that super-rss needs to support has many website has possible. Do NOT hardcode a solution for a website. Think of generic solution that might work on other website
- The plan needs to include unit-test for prevent future regression
- All the curl output should be stored in `tmp/` for further debugging


