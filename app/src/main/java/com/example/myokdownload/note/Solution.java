package com.example.myokdownload.note;

public class Solution {
    public String solve (String s, String t) {
        // write code here
        if (t.equals("0") || s.equals("0")) return "0";
        StringBuilder tag = new StringBuilder();
        String res = "";
        for (int i = t.length() - 1; i >= 0; i--) {
            if (t.charAt(i) != '0') {
                String str = solve(s, t.charAt(i)) + tag;
                res = add(res, str);
            }
            tag.append("0");
        }
        return res;
    }

    public String solve(String s, char ch) {
        int cnt = 0, b = ch - '0', i = s.length() - 1;
        StringBuilder res = new StringBuilder();
        while (i >= 0 || cnt > 0) {
            if (i >= 0) {
                cnt += (s.charAt(i--) - '0') * b;
            }
            res.insert(0, ((char) (cnt % 10 + '0')));
            cnt = cnt / 10;
        }
        return res.toString();
    }

    public String add(String s, String t) {
        int i = s.length() - 1, j = t.length() - 1, cnt = 0;
        StringBuilder res = new StringBuilder();
        while (i >= 0 || j >= 0 || cnt > 0) {
            if (i >= 0) {
                cnt += s.charAt(i--) - '0';
            }
            if (j >= 0) {
                cnt += t.charAt(j--) - '0';
            }
            res.insert(0, (char)(cnt % 10 + '0'));
            cnt = cnt / 10;
        }
        return res.toString();
    }
}