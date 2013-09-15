tiny    = load 'tiny'   as (plan:chararray, score:int, jk1:int);
colors  = load 'colors' as (plan:chararray, color:chararray, jk2:int);
colors2 = load 'colors' as (plan:chararray, color:chararray, jk2:int);
colors3 = load 'colors';
tiny_colors = join tiny by (plan + score, plan + 1, 100*200+$1, $2), colors by (plan + score, 1, 1/200+jk2, jk2);
colors_filtered = filter colors by $1 == 'red' and $1 == 'blue' or $2 == 'green' or $2 * 2 + 5 / ($2 + 3) != 3;
tiny_colors_cogrp = cogroup tiny by (score, plan) inner, colors by (jk2+1, plan), colors2 by (50, plan);
tiny_colors_join = join tiny by (score, plan), colors by (jk2+1, plan), colors2 by (50, plan), colors3 by ($3, $1);
out = limit tiny_colors_cogrp 10;
store out into 'test_out_cogrp';
store tiny_colors_join into 'test_out_join';
store tiny_colors into 'test_out_tiny_colors';
--dump out;
